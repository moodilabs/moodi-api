package com.moodi.member.presentation.dto;

import com.moodi.member.application.dto.WithdrawalCommand;
import com.moodi.member.domain.WithdrawalReason;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record WithdrawalRequest(
        @NotEmpty(message = "탈퇴 사유를 1개 이상 선택해주세요.")
        Set<WithdrawalReason> reasons,

        @Size(max = 500, message = "기타 사유는 500자 이하여야 합니다.")
        String detail
) {

    public WithdrawalCommand toCommand() {
        if (reasons.contains(WithdrawalReason.ADMIN_FORCED)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return new WithdrawalCommand(reasons, detail);
    }
}
