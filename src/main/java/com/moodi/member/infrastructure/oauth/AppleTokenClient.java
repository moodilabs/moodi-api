package com.moodi.member.infrastructure.oauth;

import com.moodi.member.application.SocialTokenClient;
import com.moodi.member.domain.OAuthProvider;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Apple 토큰 엔드포인트 어댑터. 인가 코드 → refresh token 교환, refresh token 철회.
 *
 * <p>둘 다 {@code client_secret}이 필요한데 Apple은 고정 문자열이 아니라 <b>.p8 키로 서명한 ES256 JWT</b>를 요구한다
 * ({@code iss}=Team ID, {@code sub}=client_id, {@code aud}=appleid.apple.com). 요청마다 짧게 만들어 쓴다.
 * client_id는 토큰을 발급받은 앱(id_token의 aud)과 같아야 하므로 호출자가 넘긴다.
 *
 * @see <a href="https://developer.apple.com/documentation/signinwithapple/generate-and-validate-tokens">Generate and validate tokens</a>
 * @see <a href="https://developer.apple.com/documentation/signinwithapple/revoke-tokens">Revoke tokens</a>
 */
@Slf4j
public class AppleTokenClient implements SocialTokenClient {

    static final String CLIENT_SECRET_AUDIENCE = "https://appleid.apple.com";
    /** Apple 허용 최대는 6개월이지만 요청마다 새로 만들므로 짧게 잡는다. */
    private static final Duration CLIENT_SECRET_TTL = Duration.ofMinutes(5);
    private final RestClient restClient;
    private final String teamId;
    private final String keyId;
    private final PrivateKey privateKey;
    private final String tokenUri;
    private final String revokeUri;
    private final Clock clock;

    public AppleTokenClient(AppleTokenProperties properties, RestClient.Builder builder, Clock clock) {
        this.teamId = properties.teamId();
        this.keyId = properties.keyId();
        this.privateKey = parsePrivateKey(properties.privateKey());
        this.tokenUri = properties.tokenUri();
        this.revokeUri = properties.revokeUri();
        this.clock = clock;
        this.restClient = builder.build();
    }

    @Override
    public Optional<String> exchangeRefreshToken(OAuthProvider provider, String clientId, String authorizationCode) {
        if (!supports(provider, clientId)) {
            return Optional.empty();
        }
        MultiValueMap<String, String> form = baseForm(clientId);
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        try {
            Map<?, ?> body = restClient.post().uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            Object refreshToken = body == null ? null : body.get("refresh_token");
            return Optional.ofNullable(refreshToken).map(Object::toString).filter(token -> !token.isBlank());
        } catch (RestClientResponseException e) {
            log.warn("Apple 토큰 교환 거절: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Apple 토큰 교환 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean revoke(OAuthProvider provider, String clientId, String refreshToken) {
        if (!supports(provider, clientId)) {
            return false;
        }
        MultiValueMap<String, String> form = baseForm(clientId);
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");
        try {
            restClient.post().uri(revokeUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            log.warn("Apple 토큰 철회 거절: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.warn("Apple 토큰 철회 실패: {}", e.getMessage());
            return false;
        }
    }

    /** client_id 없이는 secret을 만들 수 없다 — 이 값은 로그인 id_token의 aud에서 온다. */
    private boolean supports(OAuthProvider provider, String clientId) {
        if (provider != OAuthProvider.APPLE) {
            return false;
        }
        if (clientId == null || clientId.isBlank()) {
            log.warn("Apple client_id 없음 — 토큰 교환·철회 불가");
            return false;
        }
        return true;
    }

    private MultiValueMap<String, String> baseForm(String clientId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret(clientId));
        return form;
    }

    String clientSecret(String clientId) {
        Date now = Date.from(clock.instant());
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + CLIENT_SECRET_TTL.toMillis()))
                .audience().add(CLIENT_SECRET_AUDIENCE).and()
                .subject(clientId)
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
    }

    /** .p8은 PKCS#8 PEM. 환경변수로 받으면 개행이 {@code \n} 리터럴로 올 수 있어 먼저 되돌린다. */
    static PrivateKey parsePrivateKey(String pem) {
        if (pem == null || pem.isBlank()) {
            throw new IllegalStateException("oauth.apple.token.private-key가 비어 있습니다");
        }
        String base64 = pem.replace("\\n", "\n")
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("oauth.apple.token.private-key를 EC PKCS#8 키로 읽을 수 없습니다", e);
        }
    }
}
