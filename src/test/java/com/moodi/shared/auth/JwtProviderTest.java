package com.moodi.shared.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-for-jwt-hs256-must-be-at-least-32-bytes";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 1_800_000, 1_209_600_000, 1_800_000, 43_200_000);

    @Test
    @DisplayName("관리자 액세스 토큰은 ID와 권한을 담고 그대로 돌려준다")
    void admin_access_token_round_trips_id_and_role() {
        UUID adminId = UUID.randomUUID();

        String token = jwtProvider.issueAdminAccessToken(adminId, AdminRole.OPERATOR);
        Optional<AdminPrincipal> principal = jwtProvider.parseAdminAccessToken(token);

        assertThat(principal).contains(new AdminPrincipal(adminId, AdminRole.OPERATOR));
    }

    @Test
    @DisplayName("회원 액세스 토큰은 관리자 토큰으로 파싱되지 않는다")
    void member_access_token_is_not_admin_token() {
        String memberToken = jwtProvider.issueAccessToken(UUID.randomUUID());

        assertThat(jwtProvider.parseAdminAccessToken(memberToken)).isEmpty();
    }

    @Test
    @DisplayName("관리자 액세스 토큰은 회원 토큰으로 파싱되지 않는다")
    void admin_access_token_is_not_member_token() {
        String adminToken = jwtProvider.issueAdminAccessToken(UUID.randomUUID(), AdminRole.SUPER);

        assertThat(jwtProvider.parseAccessToken(adminToken)).isEmpty();
        assertThat(jwtProvider.parseRefreshToken(adminToken)).isEmpty();
    }

    @Test
    @DisplayName("관리자 리프레시 토큰은 액세스 토큰으로 쓸 수 없다")
    void admin_refresh_token_is_not_access_token() {
        UUID adminId = UUID.randomUUID();
        IssuedToken refresh = jwtProvider.issueAdminRefreshToken(adminId);

        assertThat(jwtProvider.parseAdminAccessToken(refresh.token())).isEmpty();
        assertThat(jwtProvider.parseAdminRefreshToken(refresh.token())).contains(adminId);
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 파싱되지 않는다")
    void token_signed_with_other_key_is_rejected() {
        JwtProvider other = new JwtProvider("another-secret-key-for-jwt-hs256-at-least-32-bytes-long",
                1_800_000, 1_209_600_000, 1_800_000, 43_200_000);
        String token = other.issueAdminAccessToken(UUID.randomUUID(), AdminRole.SUPER);

        assertThat(jwtProvider.parseAdminAccessToken(token)).isEmpty();
    }
}
