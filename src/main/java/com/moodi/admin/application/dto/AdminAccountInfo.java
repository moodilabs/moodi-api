package com.moodi.admin.application.dto;

import com.moodi.admin.domain.AdminAccount;
import com.moodi.admin.domain.AdminAccountStatus;
import com.moodi.shared.auth.AdminRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminAccountInfo(UUID id, String email, String name, AdminRole role, AdminAccountStatus status,
                               LocalDateTime lastLoginAt, LocalDateTime createdAt) {

    public static AdminAccountInfo from(AdminAccount account) {
        return new AdminAccountInfo(account.getId(), account.getEmail(), account.getName(), account.getRole(),
                account.getStatus(), account.getLastLoginAt(), account.getCreatedAt());
    }
}
