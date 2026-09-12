package com.moodi.member.domain;

public enum MemberStatus {
    PENDING,
    ACTIVE,
    /** 어드민 정지. 로그인·재발급이 막히고 해제하면 ACTIVE로 돌아간다. */
    SUSPENDED
}
