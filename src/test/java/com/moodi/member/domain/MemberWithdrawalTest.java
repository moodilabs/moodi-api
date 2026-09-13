package com.moodi.member.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberWithdrawalTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Test
    @DisplayName("사유와 상세를 기록한다 (상세는 공백이면 null)")
    void record_keeps_reasons_and_trims_detail() {
        MemberWithdrawal withdrawal = MemberWithdrawal.record(MEMBER_ID,
                Set.of(WithdrawalReason.OTHER, WithdrawalReason.HARD_TO_USE), "  hard to use  ");
        MemberWithdrawal blank = MemberWithdrawal.record(MEMBER_ID, Set.of(WithdrawalReason.NOT_USED_MUCH), "  ");

        assertThat(withdrawal.getReasons()).containsExactlyInAnyOrder(WithdrawalReason.OTHER, WithdrawalReason.HARD_TO_USE);
        assertThat(withdrawal.getDetail()).isEqualTo("hard to use");
        assertThat(blank.getDetail()).isNull();
    }

    @Test
    @DisplayName("사유가 없으면 기록할 수 없다")
    void record_requires_reason() {
        assertThatThrownBy(() -> MemberWithdrawal.record(MEMBER_ID, Set.of(), null))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.WITHDRAWAL_REASON_REQUIRED);
    }

    @Test
    @DisplayName("상세가 500자를 넘으면 기록할 수 없다")
    void record_rejects_long_detail() {
        assertThatThrownBy(() -> MemberWithdrawal.record(MEMBER_ID, Set.of(WithdrawalReason.OTHER), "x".repeat(501)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
