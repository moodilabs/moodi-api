package com.moodi.member.application.dto;

import com.moodi.member.domain.WithdrawalReason;

import java.util.Set;

/**
 * @param authorizationCode 제공자 인가 코드(선택). 로그인 때 refresh token을 받아두지 못한 회원이
 *                          탈퇴 직전 재인증으로 받은 코드를 보내면 그 자리에서 교환해 철회한다.
 */
public record WithdrawalCommand(Set<WithdrawalReason> reasons, String detail, String authorizationCode) {

    public WithdrawalCommand(Set<WithdrawalReason> reasons, String detail) {
        this(reasons, detail, null);
    }

    public static WithdrawalCommand adminForced() {
        return new WithdrawalCommand(Set.of(WithdrawalReason.ADMIN_FORCED), null);
    }
}
