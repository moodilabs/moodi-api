package com.moodi.admin.infrastructure;

import com.moodi.admin.application.AdminTokenProvider;
import com.moodi.admin.application.dto.AdminTokenPair;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.auth.IssuedToken;
import com.moodi.shared.auth.JwtProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAdminTokenProvider implements AdminTokenProvider {

    private final JwtProvider jwtProvider;

    public JwtAdminTokenProvider(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public AdminTokenPair issue(UUID adminId, AdminRole role) {
        String accessToken = jwtProvider.issueAdminAccessToken(adminId, role);
        IssuedToken refreshToken = jwtProvider.issueAdminRefreshToken(adminId);
        return new AdminTokenPair(accessToken, refreshToken.token(), refreshToken.expiresAt());
    }

    @Override
    public Optional<UUID> parseRefreshToken(String refreshToken) {
        return jwtProvider.parseAdminRefreshToken(refreshToken);
    }
}
