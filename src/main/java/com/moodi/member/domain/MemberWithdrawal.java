package com.moodi.member.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 탈퇴 기록. 회원은 재가입 후 다시 탈퇴할 수 있으므로 회원당 여러 건일 수 있다.
 * 개인정보를 담지 않으므로 회원 행이 비워져도 남는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWithdrawal extends BaseEntity {

    public static final int MAX_DETAIL_LENGTH = 500;

    private UUID id;
    private UUID memberId;
    private Set<WithdrawalReason> reasons = new HashSet<>();
    private String detail;

    private MemberWithdrawal(UUID memberId, Set<WithdrawalReason> reasons, String detail) {
        if (memberId == null || reasons == null || reasons.isEmpty()) {
            throw new BusinessException(ErrorCode.WITHDRAWAL_REASON_REQUIRED);
        }
        if (detail != null && detail.length() > MAX_DETAIL_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.memberId = memberId;
        this.reasons = EnumSet.copyOf(reasons);
        this.detail = detail == null || detail.isBlank() ? null : detail.trim();
    }

    public static MemberWithdrawal record(UUID memberId, Set<WithdrawalReason> reasons, String detail) {
        return new MemberWithdrawal(memberId, reasons, detail);
    }

    public Set<WithdrawalReason> getReasons() {
        return Collections.unmodifiableSet(reasons);
    }
}
