package com.moodi.spot.infrastructure.openai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.moodi.spot.application.LlmAdmissionController;

import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 분산 호출 보호 계층.
 *
 * <p>Lua 스크립트로 Semaphore(lease 기반) + 슬라이딩 윈도우 Rate Limiter를 원자적으로 실행한다.
 * Circuit Breaker는 인스턴스 로컬 — 연속 실패는 인스턴스별로 판단하되,
 * 동시성·호출량 제한은 모든 인스턴스가 공유한다.
 *
 * <p>Semaphore permit에는 lease TTL이 있어, release를 못 해도 자동 만료된다.
 */
@Slf4j
public class RedisAdmissionController implements LlmAdmissionController {

    private static final String SEM_KEY = "llm:semaphore";
    private static final String RATE_KEY = "llm:rate";
    private static final String COOLDOWN_KEY = "llm:cooldown";
    private static final long LEASE_MILLIS = 30_000;

    private final RedisCommands<String, String> commands;
    private final String acquireScriptSha;
    private final String releaseScriptSha;
    private final String cooldownScriptSha;
    private final int maxConcurrent;
    private final int maxRequestsPerMinute;
    private final int circuitFailureThreshold;
    private final long circuitOpenDurationMillis;

    // Circuit Breaker — 인스턴스 로컬
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenUntil = new AtomicLong(0);

    // 현재 요청의 callerId — ThreadLocal로 tryAcquire ↔ release를 연결
    private final ThreadLocal<String> currentCallerId = new ThreadLocal<>();

    public RedisAdmissionController(RedisCommands<String, String> commands,
                                    LlmProtectionProperties properties) {
        this.commands = commands;
        this.maxConcurrent = properties.maxConcurrent();
        this.maxRequestsPerMinute = properties.maxRequestsPerMinute();
        this.circuitFailureThreshold = properties.circuitFailureThreshold();
        this.circuitOpenDurationMillis = properties.circuitOpenDurationSeconds() * 1000;

        // Lua 스크립트를 Redis에 로드 (SHA 캐싱)
        this.acquireScriptSha = commands.scriptLoad(loadScript("scripts/redis/llm_acquire.lua"));
        this.releaseScriptSha = commands.scriptLoad(loadScript("scripts/redis/llm_release.lua"));
        this.cooldownScriptSha = commands.scriptLoad(loadScript("scripts/redis/llm_cooldown.lua"));
    }

    @Override
    public boolean tryAcquire(long timeoutMillis) {
        long now = System.currentTimeMillis();

        // Circuit Breaker 체크 (로컬)
        if (isCircuitOpen()) {
            log.debug("LLM 호출 거부: Circuit OPEN");
            return false;
        }

        String callerId = UUID.randomUUID().toString();
        try {
            Long result = commands.evalsha(
                    acquireScriptSha,
                    io.lettuce.core.ScriptOutputType.INTEGER,
                    new String[]{SEM_KEY, RATE_KEY, COOLDOWN_KEY},
                    String.valueOf(maxConcurrent),
                    String.valueOf(maxRequestsPerMinute),
                    callerId,
                    String.valueOf(now),
                    String.valueOf(LEASE_MILLIS)
            );
            if (result != null && result == 1L) {
                currentCallerId.set(callerId);
                return true;
            }
            log.debug("LLM 호출 거부: Redis Semaphore/Rate Limit");
            return false;
        } catch (Exception e) {
            log.warn("Redis 호출 실패 — 허가 거부로 처리: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void release() {
        String callerId = currentCallerId.get();
        if (callerId == null) {
            return;
        }
        currentCallerId.remove();
        try {
            commands.evalsha(
                    releaseScriptSha,
                    io.lettuce.core.ScriptOutputType.INTEGER,
                    new String[]{SEM_KEY},
                    callerId
            );
        } catch (Exception e) {
            log.warn("Redis permit 반납 실패 (lease TTL로 자동 만료 예정): {}", e.getMessage());
        }
    }

    @Override
    public void recordSuccess() {
        consecutiveFailures.set(0);
    }

    @Override
    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= circuitFailureThreshold) {
            long openUntil = System.currentTimeMillis() + circuitOpenDurationMillis;
            circuitOpenUntil.set(openUntil);
            log.warn("Circuit OPEN: 연속 {} 회 실패, {}초간 호출 차단",
                    failures, circuitOpenDurationMillis / 1000);
        }
    }

    @Override
    public void recordRetryAfter(long retryAfterSeconds) {
        long now = System.currentTimeMillis();
        long cooldownUntilMs = now + retryAfterSeconds * 1000;
        long ttlMs = retryAfterSeconds * 1000 + 1000;
        try {
            commands.evalsha(
                    cooldownScriptSha,
                    io.lettuce.core.ScriptOutputType.INTEGER,
                    new String[]{COOLDOWN_KEY},
                    String.valueOf(cooldownUntilMs),
                    String.valueOf(ttlMs)
            );
            log.warn("429 Retry-After: {}초간 전역 호출 거부", retryAfterSeconds);
        } catch (Exception e) {
            log.warn("Redis cooldown 설정 실패: {}", e.getMessage());
        }
    }

    @Override
    public boolean isCircuitOpen() {
        long openUntil = circuitOpenUntil.get();
        if (openUntil == 0) {
            return false;
        }
        if (System.currentTimeMillis() >= openUntil) {
            circuitOpenUntil.set(0);
            consecutiveFailures.set(0);
            log.info("Circuit HALF-OPEN → CLOSED: 호출 재개");
            return false;
        }
        return true;
    }

    private static String loadScript(String path) {
        try (InputStream is = RedisAdmissionController.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Lua 스크립트를 찾을 수 없습니다: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Lua 스크립트 읽기 실패: " + path, e);
        }
    }
}
