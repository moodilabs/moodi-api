package com.moodi.member.application;

import com.moodi.member.application.dto.LoginResult;
import com.moodi.member.application.dto.OidcPayload;
import com.moodi.member.application.dto.TokenPair;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.member.domain.RefreshToken;
import com.moodi.member.domain.RefreshTokenRepository;
import com.moodi.member.support.MemberFixture;
import com.moodi.member.support.RefreshTokenFixture;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final OAuthProvider PROVIDER = OAuthProvider.GOOGLE;
    private static final String ID_TOKEN = "id-token";
    private static final String PROVIDER_ID = "google-sub-123";
    private static final String EMAIL = "user@moodi.kr";

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private OAuthClient oAuthClient;

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("신규 회원 소셜 로그인 성공")
    void login_new_member_success() {
        UUID memberId = UUID.randomUUID();
        TokenPair tokens = new TokenPair("access-token", "refresh-token", LocalDateTime.now().plusDays(14));
        when(oAuthClient.verify(PROVIDER, ID_TOKEN)).thenReturn(new OidcPayload(PROVIDER_ID, EMAIL));
        when(memberRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID)).thenReturn(Optional.empty());
        when(memberRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(MemberFixture.create(memberId, PROVIDER, PROVIDER_ID, EMAIL));
        when(tokenProvider.issue(memberId)).thenReturn(tokens);

        LoginResult result = authService.login(PROVIDER, ID_TOKEN);

        assertThat(result.isNewMember()).isTrue();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(memberRepository).save(any(Member.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("기존 회원 소셜 로그인 성공")
    void login_existing_member_success() {
        UUID memberId = UUID.randomUUID();
        TokenPair tokens = new TokenPair("access-token", "refresh-token", LocalDateTime.now().plusDays(14));
        when(oAuthClient.verify(PROVIDER, ID_TOKEN)).thenReturn(new OidcPayload(PROVIDER_ID, EMAIL));
        when(memberRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID))
                .thenReturn(Optional.of(MemberFixture.create(memberId, PROVIDER, PROVIDER_ID, EMAIL)));
        when(tokenProvider.issue(memberId)).thenReturn(tokens);

        LoginResult result = authService.login(PROVIDER, ID_TOKEN);

        assertThat(result.isNewMember()).isFalse();
        verify(memberRepository, never()).save(any(Member.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("신규 회원 가입 시 이메일이 중복이면 예외가 발생한다")
    void login_new_member_with_duplicate_email_throws() {
        when(oAuthClient.verify(PROVIDER, ID_TOKEN)).thenReturn(new OidcPayload(PROVIDER_ID, EMAIL));
        when(memberRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID)).thenReturn(Optional.empty());
        when(memberRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(PROVIDER, ID_TOKEN))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        verify(memberRepository, never()).save(any(Member.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("이메일이 없으면 중복 검사를 건너뛰고 가입에 성공한다")
    void login_new_member_without_email_skips_duplicate_check() {
        UUID memberId = UUID.randomUUID();
        TokenPair tokens = new TokenPair("access-token", "refresh-token", LocalDateTime.now().plusDays(14));
        when(oAuthClient.verify(PROVIDER, ID_TOKEN)).thenReturn(new OidcPayload(PROVIDER_ID, null));
        when(memberRepository.findByProviderAndProviderId(PROVIDER, PROVIDER_ID)).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenReturn(MemberFixture.create(memberId, PROVIDER, PROVIDER_ID, null));
        when(tokenProvider.issue(memberId)).thenReturn(tokens);

        LoginResult result = authService.login(PROVIDER, ID_TOKEN);

        assertThat(result.isNewMember()).isTrue();
        verify(memberRepository, never()).existsByEmail(anyString());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("리프레시 토큰 재발급 성공")
    void reissue_success() {
        UUID memberId = UUID.randomUUID();
        String refreshToken = "old-refresh-token";
        TokenPair newTokens = new TokenPair("new-access-token", "new-refresh-token", LocalDateTime.now().plusDays(14));
        when(tokenProvider.parseRefreshToken(refreshToken)).thenReturn(Optional.of(memberId));
        when(refreshTokenRepository.findByToken(refreshToken))
                .thenReturn(Optional.of(RefreshTokenFixture.notExpired(memberId, refreshToken)));
        when(tokenProvider.issue(memberId)).thenReturn(newTokens);

        LoginResult result = authService.reissue(refreshToken);

        assertThat(result.isNewMember()).isFalse();
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRepository).deleteByToken(refreshToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("파싱되지 않는 리프레시 토큰이면 재발급에 실패한다")
    void reissue_with_unparsable_token_throws() {
        String refreshToken = "invalid-refresh-token";
        when(tokenProvider.parseRefreshToken(refreshToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verify(refreshTokenRepository, never()).deleteByToken(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("저장되지 않은 리프레시 토큰이면 재발급에 실패한다")
    void reissue_with_unknown_token_throws() {
        UUID memberId = UUID.randomUUID();
        String refreshToken = "unknown-refresh-token";
        when(tokenProvider.parseRefreshToken(refreshToken)).thenReturn(Optional.of(memberId));
        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verify(refreshTokenRepository, never()).deleteByToken(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("만료된 리프레시 토큰이면 재발급에 실패한다")
    void reissue_with_expired_token_throws() {
        UUID memberId = UUID.randomUUID();
        String refreshToken = "expired-refresh-token";
        when(tokenProvider.parseRefreshToken(refreshToken)).thenReturn(Optional.of(memberId));
        when(refreshTokenRepository.findByToken(refreshToken))
                .thenReturn(Optional.of(RefreshTokenFixture.expired(memberId, refreshToken)));

        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verify(refreshTokenRepository, never()).deleteByToken(anyString());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그아웃 시 회원의 리프레시 토큰을 전체 삭제한다")
    void logout_deletes_all_refresh_tokens() {
        UUID memberId = UUID.randomUUID();

        authService.logout(memberId);

        verify(refreshTokenRepository).deleteByMemberId(memberId);
    }
}
