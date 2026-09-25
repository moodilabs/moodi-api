package com.moodi.spot.domain;

public enum MoodTaggingStatus {
    /** 아직 한 번도 태깅 시도하지 않음. */
    PENDING,
    /** 다른 작업자가 처리 중 (LLM 호출 대기/진행). */
    PROCESSING,
    /** 일시 오류(timeout, 429, 5xx)로 자동 재시도 대기. */
    RETRY_WAIT,
    /** 태깅 성공. */
    COMPLETED,
    /** 재시도 횟수 초과 또는 데이터 자체 문제로 수동 확인 필요. */
    FAILED
}
