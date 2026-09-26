package com.moodi.member.infrastructure.oauth;

import com.moodi.member.application.SocialTokenClient;
import com.moodi.member.domain.OAuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

/**
 * Google OAuth 토큰 엔드포인트 어댑터. serverAuthCode → refresh token 교환, refresh token 철회.
 *
 * <p>refresh token을 철회하면 그 승인(grant) 전체가 취소돼 Google 계정의 "타사 앱 및 서비스" 목록에서 앱이 사라진다.
 * refresh token은 사용자가 <b>처음 동의할 때만</b> 내려오므로 클라이언트는 코드를 요청할 때
 * {@code forceCodeForRefreshToken}(Android)·{@code access_type=offline&prompt=consent}(웹)로 매번 받도록 해야 한다.
 * Google 철회는 client_id가 필요 없어 호출자가 넘긴 clientId는 쓰지 않는다.
 *
 * @see <a href="https://developers.google.com/identity/protocols/oauth2/web-server#tokenrevoke">Revoking a token</a>
 */
@Slf4j
public class GoogleTokenClient implements SocialTokenClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUri;
    private final String revokeUri;
    private final String redirectUri;

    public GoogleTokenClient(GoogleTokenProperties properties, RestClient.Builder builder) {
        this.clientId = properties.clientId();
        this.clientSecret = properties.clientSecret();
        this.tokenUri = properties.tokenUri();
        this.revokeUri = properties.revokeUri();
        this.redirectUri = properties.redirectUri();
        this.restClient = builder.build();
    }

    @Override
    public Optional<String> exchangeRefreshToken(OAuthProvider provider, String ignoredClientId, String authorizationCode) {
        if (provider != OAuthProvider.GOOGLE) {
            return Optional.empty();
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        form.add("redirect_uri", redirectUri == null ? "" : redirectUri);
        try {
            OAuthTokenResponse body = restClient.post().uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(OAuthTokenResponse.class);
            if (body == null || body.refreshToken() == null) {
                // 재동의 없이 받은 코드는 access_token만 온다 — 클라이언트가 강제 동의 옵션을 빠뜨린 것
                log.warn("Google 토큰 응답에 refresh_token 없음 — 클라이언트의 serverAuthCode 요청 옵션 확인 필요");
                return Optional.empty();
            }
            return Optional.of(body.refreshToken()).filter(token -> !token.isBlank());
        } catch (RestClientResponseException e) {
            log.warn("Google 토큰 교환 거절: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Google 토큰 교환 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean revoke(OAuthProvider provider, String ignoredClientId, String refreshToken) {
        if (provider != OAuthProvider.GOOGLE) {
            return false;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", refreshToken);
        try {
            restClient.post().uri(revokeUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            log.warn("Google 토큰 철회 거절: status={}, body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.warn("Google 토큰 철회 실패: {}", e.getMessage());
            return false;
        }
    }
}
