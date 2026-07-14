package com.moodi.member.domain;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByMemberId(UUID memberId);
}
