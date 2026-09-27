package com.moodi.spot.infrastructure.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM 호출 보호 설정.
 *
 * <p>{@code mode}는 운영 환경의 인스턴스 전략과 쌍을 이룬다.
 * <ul>
 *   <li>{@code local} — in-memory 제어. Cloud Run max instances=1일 때만 사용</li>
 *   <li>{@code redis} — 분산 제어. 다중 인스턴스 운영 시 필수</li>
 * </ul>
 *
 * @param mode           보호 모드 ({@code local} | {@code redis})
 * @param maxConcurrent  동시 호출 허용 수 (Semaphore permit)
 * @param acquireTimeoutMillis  Semaphore 획득 대기 시간 (밀리초)
 * @param maxRequestsPerMinute  분당 최대 호출 수 (Rate Limiter)
 * @param circuitFailureThreshold  Circuit Breaker 연속 실패 임계값
 * @param circuitOpenDurationSeconds  Circuit Open 유지 시간 (초)
 */
@ConfigurationProperties(prefix = "moodi.llm.protection")
public record LlmProtectionProperties(
        String mode,
        int maxConcurrent,
        long acquireTimeoutMillis,
        int maxRequestsPerMinute,
        int circuitFailureThreshold,
        long circuitOpenDurationSeconds
) {

    public LlmProtectionProperties {
        if (mode == null) {
            mode = "local";
        }
        if (maxConcurrent <= 0) {
            maxConcurrent = 2;
        }
        if (acquireTimeoutMillis <= 0) {
            acquireTimeoutMillis = 10_000;
        }
        if (maxRequestsPerMinute <= 0) {
            maxRequestsPerMinute = 20;
        }
        if (circuitFailureThreshold <= 0) {
            circuitFailureThreshold = 5;
        }
        if (circuitOpenDurationSeconds <= 0) {
            circuitOpenDurationSeconds = 30;
        }
    }
}
