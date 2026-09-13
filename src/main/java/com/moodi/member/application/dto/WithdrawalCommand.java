package com.moodi.member.application.dto;

import com.moodi.member.domain.WithdrawalReason;

import java.util.Set;

public record WithdrawalCommand(Set<WithdrawalReason> reasons, String detail) {

    public static WithdrawalCommand adminForced() {
        return new WithdrawalCommand(Set.of(WithdrawalReason.ADMIN_FORCED), null);
    }
}
