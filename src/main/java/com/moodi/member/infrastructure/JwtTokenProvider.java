package com.moodi.member.infrastructure;

import com.moodi.member.application.TokenProvider;
import com.moodi.member.application.dto.TokenPair;
import com.moodi.shared.auth.IssuedToken;
import com.moodi.shared.auth.JwtProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider implements TokenProvider {

    private final JwtProvider jwtProvider;

    public JwtTokenProvider(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public TokenPair issue(UUID memberId) {
        String accessToken = jwtProvider.issueAccessToken(memberId);
        IssuedToken refreshToken = jwtProvider.issueRefreshToken(memberId);
        return new TokenPair(accessToken, refreshToken.token(), refreshToken.expiresAt());
    }

    @Override
    public Optional<UUID> parseRefreshToken(String refreshToken) {
        return jwtProvider.parseRefreshToken(refreshToken);
    }
}
