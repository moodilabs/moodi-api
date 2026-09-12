package com.moodi.admin.presentation.dto;

import com.moodi.shared.auth.AdminRole;
import jakarta.validation.constraints.NotNull;

public record AdminAccountRoleRequest(
        @NotNull(message = "권한은 필수입니다.")
        AdminRole role
) {
}
