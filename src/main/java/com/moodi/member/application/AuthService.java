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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthClient oAuthClient;
    private final TokenProvider tokenProvider;

    public AuthService(
            MemberRepository memberRepository,
            RefreshTokenRepository refreshTokenRepository,
            OAuthClient oAuthClient,
            TokenProvider tokenProvider
    ) {
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.oAuthClient = oAuthClient;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public LoginResult login(OAuthProvider provider, String idToken) {
        OidcPayload payload = oAuthClient.verify(provider, idToken);
        MemberResolution resolution = resolveMember(provider, payload);
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
        refreshTokenRepository.deleteByToken(refreshToken);
        TokenPair tokens = issueTokens(memberId);
        return new LoginResult(tokens.accessToken(), tokens.refreshToken(), false);
    }

    @Transactional
    public void logout(UUID memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    private MemberResolution resolveMember(OAuthProvider provider, OidcPayload payload) {
        return memberRepository.findByProviderAndProviderId(provider, payload.providerId())
                .map(member -> new MemberResolution(member, false))
                .orElseGet(() -> new MemberResolution(register(provider, payload), true));
    }

    private Member register(OAuthProvider provider, OidcPayload payload) {
        String email = payload.email();
        if (email != null && memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        return memberRepository.save(Member.create(provider, payload.providerId(), email));
    }

    private TokenPair issueTokens(UUID memberId) {
        TokenPair tokens = tokenProvider.issue(memberId);
        refreshTokenRepository.save(RefreshToken.issue(memberId, tokens.refreshToken(), tokens.refreshTokenExpiresAt()));
        return tokens;
    }

    private record MemberResolution(Member member, boolean isNew) {
    }
}
