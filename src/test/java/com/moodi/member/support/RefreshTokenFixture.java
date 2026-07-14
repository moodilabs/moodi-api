package com.moodi.member.support;

import com.moodi.member.domain.RefreshToken;

import java.time.LocalDateTime;
import java.util.UUID;

public class RefreshTokenFixture {

    private static final String DEFAULT_TOKEN = "refresh-token-value";

    public static RefreshToken issue(UUID memberId, String token, LocalDateTime expiresAt) {
        return RefreshToken.issue(memberId, token, expiresAt);
    }

    public static RefreshToken notExpired(UUID memberId, String token) {
        return RefreshToken.issue(memberId, token, LocalDateTime.now().plusDays(14));
    }

    public static RefreshToken expired(UUID memberId, String token) {
        return RefreshToken.issue(memberId, token, LocalDateTime.now().minusDays(1));
    }

    private RefreshTokenFixture() {
    }
}
