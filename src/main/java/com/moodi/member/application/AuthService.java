package com.moodi.member.application;

import com.moodi.member.application.dto.LoginResult;
import com.moodi.member.application.dto.OidcPayload;
import com.moodi.member.application.dto.TokenPair;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.member.domain.RefreshToken;
import com.moodi.member.domain.RefreshTokenRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthClient oAuthClient;
    private final SocialTokenClient socialTokenClient;
    private final TokenProvider tokenProvider;

    public AuthService(
            MemberRepository memberRepository,
            RefreshTokenRepository refreshTokenRepository,
            OAuthClient oAuthClient,
            SocialTokenClient socialTokenClient,
            TokenProvider tokenProvider
    ) {
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.oAuthClient = oAuthClient;
        this.socialTokenClient = socialTokenClient;
        this.tokenProvider = tokenProvider;
    }

    /**
     * @param authorizationCode 제공자 인가 코드(선택). Apple은 이 코드를 refresh token으로 바꿔 두어야
     *                          탈퇴 때 계정 연결을 철회할 수 있다. 교환 실패는 로그인을 막지 않는다.
     */
    @Transactional
    public LoginResult login(OAuthProvider provider, String idToken, String authorizationCode) {
        OidcPayload payload = oAuthClient.verify(provider, idToken);
        MemberResolution resolution = resolveMember(provider, payload);
        rememberProviderCredential(resolution.member(), provider, payload.audience(), authorizationCode);
        TokenPair tokens = issueTokens(resolution.member().getId());
        return new LoginResult(tokens.accessToken(), tokens.refreshToken(), resolution.isNew());
    }

    @Transactional
    public LoginResult reissue(String refreshToken) {
        UUID memberId = tokenProvider.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (stored.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        rejectSuspended(memberId);
        refreshTokenRepository.deleteByToken(refreshToken);
        TokenPair tokens = issueTokens(memberId);
        return new LoginResult(tokens.accessToken(), tokens.refreshToken(), false);
    }

    @Transactional
    public void logout(UUID memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    /** 정지된 회원은 재발급도 막는다. 액세스 토큰은 30분 내 자연 만료된다. */
    private void rejectSuspended(UUID memberId) {
        memberRepository.findById(memberId)
                .filter(Member::isSuspended)
                .ifPresent(member -> {
                    throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
                });
    }

    private MemberResolution resolveMember(OAuthProvider provider, OidcPayload payload) {
        return memberRepository.findByProviderAndProviderId(provider, payload.providerId())
                .map(member -> resolveExisting(member, payload))
                .orElseGet(() -> new MemberResolution(register(provider, payload), true));
    }

    /**
     * 탈퇴한 회원이 같은 소셜 계정으로 돌아오면 회원 행을 재활용한다. 활동 데이터는 탈퇴 시 지워졌고
     * 프로필도 비어 있으므로 온보딩을 다시 밟아야 한다 → 신규 로그인과 같은 분기를 타도록
     * {@code isNew = true}로 돌려준다(`AUT-F01`).
     */
    private MemberResolution resolveExisting(Member member, OidcPayload payload) {
        if (member.isWithdrawn()) {
            member.restore(payload.email());
            return new MemberResolution(member, true);
        }
        if (member.isSuspended()) {
            throw new BusinessException(ErrorCode.MEMBER_SUSPENDED);
        }
        return new MemberResolution(member, false);
    }

    private Member register(OAuthProvider provider, OidcPayload payload) {
        String email = payload.email();
        if (email != null && memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        return memberRepository.save(Member.create(provider, payload.providerId(), email));
    }

    private void rememberProviderCredential(Member member, OAuthProvider provider, String clientId, String authorizationCode) {
        String refreshToken = null;
        if (authorizationCode != null && !authorizationCode.isBlank()) {
            refreshToken = socialTokenClient.exchangeRefreshToken(provider, clientId, authorizationCode).orElse(null);
            if (refreshToken == null) {
                log.warn("제공자 refresh token 교환 실패 — 탈퇴 시 계정 연결 철회가 불가할 수 있음: provider={}, memberId={}",
                        provider, member.getId());
            }
        }
        member.rememberProviderCredential(clientId, refreshToken);
    }

    private TokenPair issueTokens(UUID memberId) {
        TokenPair tokens = tokenProvider.issue(memberId);
        refreshTokenRepository.save(RefreshToken.issue(memberId, tokens.refreshToken(), tokens.refreshTokenExpiresAt()));
        return tokens;
    }

    private record MemberResolution(Member member, boolean isNew) {
    }
}
