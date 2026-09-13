package com.moodi.admin.presentation.dto;

import com.moodi.admin.application.dto.AdminAccountInfo;
import com.moodi.admin.domain.AdminAccountStatus;
import com.moodi.shared.auth.AdminRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminAccountResponse(UUID id, String loginId, String name, AdminRole role, AdminAccountStatus status,
                                   boolean passwordChangeRequired, LocalDateTime lastLoginAt,
                                   LocalDateTime createdAt) {

    public static AdminAccountResponse from(AdminAccountInfo info) {
        return new AdminAccountResponse(info.id(), info.loginId(), info.name(), info.role(), info.status(),
                info.passwordChangeRequired(), info.lastLoginAt(), info.createdAt());
    }
}
