package com.moodi.member.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sign in with Apple 토큰 교환·철회용 자격(Apple Developer > Keys > "Sign in with Apple" 키).
 *
 * @param enabled    false면 철회를 시도하지 않는다(로컬·미구성 환경). 운영은 반드시 true.
 * @param teamId     Apple Developer Team ID (10자)
 * @param keyId      .p8 키의 Key ID
 * @param privateKey .p8 파일 내용(PEM). 환경변수로 넣을 때 개행이 {@code \n} 리터럴이어도 된다.
 */
@ConfigurationProperties("oauth.apple.token")
public record AppleTokenProperties(boolean enabled, String teamId, String keyId, String privateKey,
                                   String tokenUri, String revokeUri) {
}
