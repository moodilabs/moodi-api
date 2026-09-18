package com.moodi.member.infrastructure.oauth;

import com.moodi.member.domain.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleTokenClientTest {

    private static final String TOKEN_URI = "https://google.test/token";
    private static final String REVOKE_URI = "https://google.test/revoke";

    private MockRestServiceServer server;
    private GoogleTokenClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GoogleTokenClient(
                new GoogleTokenProperties(true, "web-client-id", "web-secret", TOKEN_URI, REVOKE_URI, ""), builder);
    }

    @Test
    @DisplayName("serverAuthCode는 id_token의 aud가 아니라 설정된 웹 클라이언트 ID·secret으로 교환한다")
    void exchange_uses_configured_web_client() {
        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formDataContains(Map.of(
                        "client_id", "web-client-id", "client_secret", "web-secret",
                        "grant_type", "authorization_code", "code", "server-auth-code", "redirect_uri", "")))
                .andRespond(withSuccess("{\"access_token\":\"a\",\"refresh_token\":\"g-refresh\"}", MediaType.APPLICATION_JSON));

        assertThat(client.exchangeRefreshToken(OAuthProvider.GOOGLE, "ios-client-id.apps", "server-auth-code")).contains("g-refresh");
        server.verify();
    }

    @Test
    @DisplayName("재동의 없이 받은 코드는 refresh_token이 없다 - empty로 돌려 로그인은 계속")
    void exchange_without_refresh_token_returns_empty() {
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{\"access_token\":\"a\",\"expires_in\":3599}", MediaType.APPLICATION_JSON));

        assertThat(client.exchangeRefreshToken(OAuthProvider.GOOGLE, null, "code")).isEmpty();
    }

    @Test
    @DisplayName("refresh token 철회는 token만 보내며 200이면 성공")
    void revoke_posts_token() {
        server.expect(requestTo(REVOKE_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of("token", "g-refresh")))
                .andRespond(withSuccess());

        assertThat(client.revoke(OAuthProvider.GOOGLE, null, "g-refresh")).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("이미 철회된 토큰(invalid_token 400)은 false")
    void revoke_invalid_token_returns_false() {
        server.expect(requestTo(REVOKE_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\":\"invalid_token\"}").contentType(MediaType.APPLICATION_JSON));

        assertThat(client.revoke(OAuthProvider.GOOGLE, null, "stale")).isFalse();
    }

    @Test
    @DisplayName("다른 제공자 요청은 호출 없이 실패한다")
    void other_provider_is_ignored() {
        assertThat(client.exchangeRefreshToken(OAuthProvider.APPLE, "x", "code")).isEmpty();
        assertThat(client.revoke(OAuthProvider.APPLE, "x", "t")).isFalse();
        server.verify();
    }
}
