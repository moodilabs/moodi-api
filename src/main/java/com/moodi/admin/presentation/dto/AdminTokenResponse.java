package com.moodi.admin.presentation.dto;

import com.moodi.admin.application.dto.AdminLoginResult;
import com.moodi.shared.auth.AdminRole;

public record AdminTokenResponse(String accessToken, String refreshToken, AdminRole role) {

    public static AdminTokenResponse from(AdminLoginResult result) {
        return new AdminTokenResponse(result.accessToken(), result.refreshToken(), result.role());
    }
}
