package com.moodi.admin.presentation.dto;

import com.moodi.admin.domain.AdminAccountStatus;
import jakarta.validation.constraints.NotNull;

public record AdminAccountStatusRequest(
        @NotNull(message = "상태는 필수입니다.")
        AdminAccountStatus status
) {
}
