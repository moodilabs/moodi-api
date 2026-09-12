package com.moodi.member.application.dto;

import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberStatus;

/**
 * 어드민이 보는 회원 상태. DB의 {@link MemberStatus}에 "탈퇴"를 더한 것 —
 * 탈퇴는 `deleted_at`으로 표현되므로 도메인 enum에는 없다.
 */
public enum MemberAdminStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    WITHDRAWN;

    public static MemberAdminStatus of(Member member) {
        return of(member.getStatus(), member.isWithdrawn());
    }

    public static MemberAdminStatus of(MemberStatus status, boolean withdrawn) {
        if (withdrawn) {
            return WITHDRAWN;
        }
        return valueOf(status.name());
    }
}
