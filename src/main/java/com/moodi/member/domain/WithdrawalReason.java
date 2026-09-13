package com.moodi.member.domain;

/** 탈퇴 사유(`MY-03-02`). `ADMIN_FORCED`는 어드민 강제 탈퇴 전용이라 앱에서는 받지 않는다. */
public enum WithdrawalReason {
    NOT_USED_MUCH,
    RECOMMENDATION_MISMATCH,
    HARD_TO_USE,
    FOUND_ANOTHER_APP,
    OTHER,
    ADMIN_FORCED
}
