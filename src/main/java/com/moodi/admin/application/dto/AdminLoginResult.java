package com.moodi.admin.application.dto;

import com.moodi.shared.auth.AdminRole;

public record AdminLoginResult(String accessToken, String refreshToken, AdminRole role,
                               boolean passwordChangeRequired) {
}
