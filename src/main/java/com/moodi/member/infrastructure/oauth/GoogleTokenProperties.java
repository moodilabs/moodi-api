package com.moodi.member.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google 서버측 토큰 교환·철회용 자격. 클라이언트가 보내는 serverAuthCode는 <b>웹(서버) OAuth 클라이언트</b> 앞으로
 * 발급되므로 id_token의 aud(iOS·Android 클라이언트 ID)가 아니라 여기 적은 웹 클라이언트 ID·secret으로 교환한다.
 *
 * @param enabled      false면 철회를 시도하지 않는다.
 * @param clientId     Google Cloud 콘솔의 웹 애플리케이션 OAuth 클라이언트 ID
 * @param clientSecret 그 클라이언트의 secret
 * @param redirectUri  모바일 serverAuthCode 교환은 빈 값. 웹 GIS 코드 흐름이면 {@code postmessage}.
 */
@ConfigurationProperties("oauth.google.token")
public record GoogleTokenProperties(boolean enabled, String clientId, String clientSecret,
                                    String tokenUri, String revokeUri, String redirectUri) {
}
