package com.moodi.admin.application;

import com.moodi.admin.application.dto.AdminTokenPair;
import com.moodi.shared.auth.AdminRole;

import java.util.Optional;
import java.util.UUID;

public interface AdminTokenProvider {

    AdminTokenPair issue(UUID adminId, AdminRole role);

    Optional<UUID> parseRefreshToken(String refreshToken);
}
