package com.moodi.admin.presentation.dto;

import com.moodi.admin.application.dto.AdminAccountInfo;
import com.moodi.admin.domain.AdminAccountStatus;
import com.moodi.shared.auth.AdminRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminAccountResponse(UUID id, String email, String name, AdminRole role, AdminAccountStatus status,
                                   LocalDateTime lastLoginAt, LocalDateTime createdAt) {

    public static AdminAccountResponse from(AdminAccountInfo info) {
        return new AdminAccountResponse(info.id(), info.email(), info.name(), info.role(), info.status(),
                info.lastLoginAt(), info.createdAt());
    }
}
