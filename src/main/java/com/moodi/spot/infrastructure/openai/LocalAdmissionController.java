package com.moodi.spot.infrastructure.openai;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.moodi.spot.application.LlmAdmissionController;

import lombok.extern.slf4j.Slf4j;

/**
 * 단일 인스턴스용 in-memory 보호 계층.
 *
 * <p>Semaphore + 슬라이딩 윈도우 Rate Limiter + Circuit Breaker를 하나로 묶는다.
 * Cloud Run max instances=1 전제에서만 사용해야 한다.
 */
@Slf4j
public class LocalAdmissionController implements LlmAdmissionController {

    private final Semaphore semaphore;
    private final int maxRequestsPerMinute;
    private final int circuitFailureThreshold;
    private final long circuitOpenDurationMillis;

    // Rate Limiter: 슬라이딩 윈도우 (1분)
    private final long[] requestTimestamps;
    private int timestampIndex = 0;
    private final Object rateLimitLock = new Object();

    // Circuit Breaker
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenUntil = new AtomicLong(0);

    // 429 Retry-After cooldown
    private final AtomicLong cooldownUntil = new AtomicLong(0);

    public LocalAdmissionController(LlmProtectionProperties properties) {
        this.semaphore = new Semaphore(properties.maxConcurrent(), true);
        this.maxRequestsPerMinute = properties.maxRequestsPerMinute();
        this.circuitFailureThreshold = properties.circuitFailureThreshold();
        this.circuitOpenDurationMillis = properties.circuitOpenDurationSeconds() * 1000;
        this.requestTimestamps = new long[maxRequestsPerMinute];
    }

    @Override
    public boolean tryAcquire(long timeoutMillis) {
        long now = System.currentTimeMillis();

        // 1. 429 cooldown 체크
        if (now < cooldownUntil.get()) {
            log.debug("LLM 호출 거부: Retry-After cooldown 중 ({}ms 남음)", cooldownUntil.get() - now);
            return false;
        }

        // 2. Circuit Breaker 체크
        if (isCircuitOpen()) {
            log.debug("LLM 호출 거부: Circuit OPEN ({}ms 남음)", circuitOpenUntil.get() - now);
            return false;
        }

        // 3. Rate Limiter 체크
        synchronized (rateLimitLock) {
            long windowStart = now - 60_000;
            if (requestTimestamps[timestampIndex] > windowStart) {
                log.debug("LLM 호출 거부: 분당 {} 회 초과", maxRequestsPerMinute);
                return false;
            }
        }

        // 4. Semaphore 획득
        try {
            boolean acquired = semaphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
            if (acquired) {
                synchronized (rateLimitLock) {
                    requestTimestamps[timestampIndex] = now;
                    timestampIndex = (timestampIndex + 1) % maxRequestsPerMinute;
                }
            } else {
                log.debug("LLM 호출 거부: Semaphore 획득 타임아웃 ({}ms)", timeoutMillis);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void release() {
        semaphore.release();
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
            log.warn("Circuit OPEN: 연속 {} 회 실패, {}초간 호출 차단", failures, circuitOpenDurationMillis / 1000);
        }
    }

    @Override
    public void recordRetryAfter(long retryAfterSeconds) {
        long until = System.currentTimeMillis() + retryAfterSeconds * 1000;
        cooldownUntil.set(until);
        log.warn("429 Retry-After: {}초간 신규 호출 거부", retryAfterSeconds);
    }

    @Override
    public boolean isCircuitOpen() {
        long openUntil = circuitOpenUntil.get();
        if (openUntil == 0) {
            return false;
        }
        if (System.currentTimeMillis() >= openUntil) {
            // Half-Open: 한 번 시도 허용
            circuitOpenUntil.set(0);
            consecutiveFailures.set(0);
            log.info("Circuit HALF-OPEN → CLOSED: 호출 재개");
            return false;
        }
        return true;
    }
}
