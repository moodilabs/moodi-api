package com.moodi.member.application;

import com.moodi.member.domain.OAuthProvider;

import java.util.Optional;

/**
 * 소셜 제공자와의 계정 연결 토큰 발급·철회 포트.
 *
 * <p>Apple은 앱이 계정을 삭제할 때 Sign in with Apple 토큰을 철회하도록 요구한다(App Store 심사 지침 5.1.1(v)).
 * 철회하지 않으면 사용자 Apple ID 설정의 "Apple로 로그인" 목록에 앱이 계속 남는다.
 * 두 메서드 모두 best-effort다 — 로그인·탈퇴 자체를 막지 않도록 실패는 empty/false로 돌려준다.
 */
public interface SocialTokenClient {

    /** 인가 코드를 제공자 refresh token으로 교환한다. 제공자가 미지원이거나 교환에 실패하면 empty. */
    Optional<String> exchangeRefreshToken(OAuthProvider provider, String clientId, String authorizationCode);

    /** 제공자 쪽 계정 연결을 철회한다. 성공하면 true. */
    boolean revoke(OAuthProvider provider, String clientId, String refreshToken);
}
