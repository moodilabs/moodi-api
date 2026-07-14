package com.moodi.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final String TOKEN = "refresh-token-value";
    private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    @DisplayName("리프레시 토큰 발급 시 필드가 세팅된다")
    void issue_sets_fields() {
        RefreshToken refreshToken = RefreshToken.issue(MEMBER_ID, TOKEN, EXPIRES_AT);

        assertThat(refreshToken.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(refreshToken.getToken()).isEqualTo(TOKEN);
        assertThat(refreshToken.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("만료 시각 이전이면 만료되지 않았다")
    void is_expired_returns_false_before_expiry() {
        RefreshToken refreshToken = RefreshToken.issue(MEMBER_ID, TOKEN, EXPIRES_AT);

        assertThat(refreshToken.isExpired(EXPIRES_AT.minusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("만료 시각 이후면 만료되었다")
    void is_expired_returns_true_after_expiry() {
        RefreshToken refreshToken = RefreshToken.issue(MEMBER_ID, TOKEN, EXPIRES_AT);

        assertThat(refreshToken.isExpired(EXPIRES_AT.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("만료 시각과 정확히 같으면 만료되지 않았다")
    void is_expired_returns_false_at_exact_expiry() {
        RefreshToken refreshToken = RefreshToken.issue(MEMBER_ID, TOKEN, EXPIRES_AT);

        assertThat(refreshToken.isExpired(EXPIRES_AT)).isFalse();
    }
}
