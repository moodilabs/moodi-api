package com.moodi.spot.application;

/**
 * LLM API 호출 진입 제어 포트.
 *
 * <p>Semaphore(동시 호출 수)와 Rate Limiter(분당 호출 수)를 하나로 묶어,
 * 호출 가능 여부를 판단하고 결과(성공/실패)를 기록한다.
 *
 * <p>구현체는 {@code local}(in-memory)과 {@code redis}(분산) 두 가지이며,
 * {@code moodi.llm.protection-mode} 설정값으로 선택한다.
 */
public interface LlmAdmissionController {

    /**
     * 호출 허가를 요청한다.
     *
     * @param timeoutMillis 최대 대기 시간 (밀리초)
     * @return 허가를 받았으면 {@code true}
     */
    boolean tryAcquire(long timeoutMillis);

    /**
     * 허가를 반납한다. {@link #tryAcquire}가 {@code true}를 반환한 경우에만 호출해야 한다.
     */
    void release();

    /**
     * 호출 성공을 기록한다. Circuit Breaker 실패 카운터를 리셋한다.
     */
    void recordSuccess();

    /**
     * 호출 실패를 기록한다. 연속 실패 횟수가 임계값을 넘으면 Circuit을 연다.
     */
    void recordFailure();

    /**
     * 429 응답의 Retry-After 값을 기록한다. 해당 시간 동안 신규 호출을 거부한다.
     *
     * @param retryAfterSeconds Retry-After 헤더 값 (초)
     */
    void recordRetryAfter(long retryAfterSeconds);

    /**
     * 현재 Circuit이 열려 있는지 (호출 차단 상태인지) 반환한다.
     */
    boolean isCircuitOpen();
}
