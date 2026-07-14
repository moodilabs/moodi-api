package com.moodi.member.application;

import com.moodi.member.application.dto.TokenPair;

import java.util.Optional;
import java.util.UUID;

public interface TokenProvider {

    TokenPair issue(UUID memberId);

    Optional<UUID> parseRefreshToken(String refreshToken);
}
