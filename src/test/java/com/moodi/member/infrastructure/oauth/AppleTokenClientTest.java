package com.moodi.member.infrastructure.oauth;

import com.moodi.member.domain.OAuthProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppleTokenClientTest {

    private static final String TOKEN_URI = "https://apple.test/auth/token";
    private static final String REVOKE_URI = "https://apple.test/auth/revoke";
    private static final String CLIENT_ID = "kr.moodi.app";
    private static final Clock CLOCK = Clock.systemUTC();

    private KeyPair keyPair;
    private MockRestServiceServer server;
    private AppleTokenClient client;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        keyPair = generator.generateKeyPair();

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AppleTokenClient(properties(pem(keyPair)), builder, CLOCK);
    }

    @Test
    @DisplayName("client_secret은 .p8 키로 서명한 ES256 JWT - iss=Team ID, sub=client_id, aud=appleid.apple.com, kid=Key ID")
    void client_secret_is_es256_jwt_signed_with_p8_key() {
        String secret = client.clientSecret(CLIENT_ID);

        Jws<Claims> jws = Jwts.parser().verifyWith(keyPair.getPublic()).build().parseSignedClaims(secret);
        assertThat(jws.getHeader().getKeyId()).isEqualTo("KEY123");
        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("ES256");
        assertThat(jws.getPayload().getIssuer()).isEqualTo("TEAM123");
        assertThat(jws.getPayload().getSubject()).isEqualTo(CLIENT_ID);
        assertThat(jws.getPayload().getAudience()).containsExactly(AppleTokenClient.CLIENT_SECRET_AUDIENCE);
        assertThat(jws.getPayload().getExpiration()).isAfter(jws.getPayload().getIssuedAt());
    }

    @Test
    @DisplayName("인가 코드를 토큰 엔드포인트에 교환해 refresh_token을 돌려준다")
    void exchange_returns_refresh_token() {
        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formDataContains(Map.of(
                        "client_id", CLIENT_ID, "grant_type", "authorization_code", "code", "auth-code")))
                .andExpect(request -> assertThat(((MockClientHttpRequest) request).getBodyAsString()).contains("client_secret=eyJ"))
                .andRespond(withSuccess("{\"access_token\":\"a\",\"refresh_token\":\"r-token\",\"id_token\":\"i\"}", MediaType.APPLICATION_JSON));

        Optional<String> refreshToken = client.exchangeRefreshToken(OAuthProvider.APPLE, CLIENT_ID, "auth-code");

        assertThat(refreshToken).contains("r-token");
        server.verify();
    }

    @Test
    @DisplayName("Apple이 교환을 거절하면(invalid_grant 등) 예외 대신 empty - 로그인은 계속돼야 한다")
    void exchange_rejected_returns_empty() {
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\":\"invalid_grant\"}").contentType(MediaType.APPLICATION_JSON));

        assertThat(client.exchangeRefreshToken(OAuthProvider.APPLE, CLIENT_ID, "expired-code")).isEmpty();
    }

    @Test
    @DisplayName("refresh token 철회는 token_type_hint=refresh_token으로 보내고 200이면 성공")
    void revoke_sends_refresh_token_hint() {
        server.expect(requestTo(REVOKE_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of(
                        "client_id", CLIENT_ID, "token", "r-token", "token_type_hint", "refresh_token")))
                .andExpect(request -> assertThat(((MockClientHttpRequest) request).getBodyAsString()).contains("client_secret=eyJ"))
                .andRespond(withSuccess());

        assertThat(client.revoke(OAuthProvider.APPLE, CLIENT_ID, "r-token")).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("철회 거절은 false - 탈퇴 자체는 막지 않는다")
    void revoke_rejected_returns_false() {
        server.expect(requestTo(REVOKE_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\":\"invalid_client\"}").contentType(MediaType.APPLICATION_JSON));

        assertThat(client.revoke(OAuthProvider.APPLE, CLIENT_ID, "r-token")).isFalse();
    }

    @Test
    @DisplayName("client_id가 없으면 secret을 만들 수 없어 호출 없이 실패한다")
    void missing_client_id_skips_call() {
        assertThat(client.revoke(OAuthProvider.APPLE, null, "r-token")).isFalse();
        assertThat(client.exchangeRefreshToken(OAuthProvider.APPLE, " ", "code")).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("환경변수로 넣은 PEM은 개행이 \\\\n 리터럴이어도 읽는다")
    void parses_pem_with_escaped_newlines() {
        String escaped = pem(keyPair).replace("\n", "\\n");

        assertThat(AppleTokenClient.parsePrivateKey(escaped).getAlgorithm()).isEqualTo("EC");
    }

    @Test
    @DisplayName("키가 비어 있거나 EC 키가 아니면 기동 시점에 실패한다")
    void invalid_key_fails_fast() {
        assertThatThrownBy(() -> AppleTokenClient.parsePrivateKey("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> AppleTokenClient.parsePrivateKey("-----BEGIN PRIVATE KEY-----\nbm90LWEta2V5\n-----END PRIVATE KEY-----"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static AppleTokenProperties properties(String privateKeyPem) {
        return new AppleTokenProperties(true, "TEAM123", "KEY123", privateKeyPem, TOKEN_URI, REVOKE_URI);
    }

    private static String pem(KeyPair keyPair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
    }
}
