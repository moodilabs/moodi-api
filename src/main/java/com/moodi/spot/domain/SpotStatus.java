package com.moodi.spot.domain;

/**
 * 앱에는 {@link #PUBLISHED}만 노출된다 — 모든 앱 조회가 `status = 'PUBLISHED'`를 건다.
 * 그래서 어드민이 스팟을 숨기거나 지우면(COM-01-02/03 "This spot is no longer available") 상태만 바꾸면 된다.
 */
public enum SpotStatus {
    /** 수집됐지만 무드 태깅 전. 배치가 태깅을 마치면 PUBLISHED로. */
    TAGGING_PENDING,
    PUBLISHED,
    /** 어드민이 잠시 내림(폐업 확인 중 등). 다시 PUBLISHED로 돌릴 수 있다. */
    HIDDEN,
    /** 어드민이 영구 삭제 처리. 원본 행은 남지만 되돌리지 않는다. */
    DELETED
}
