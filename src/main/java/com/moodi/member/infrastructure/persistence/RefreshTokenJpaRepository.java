package com.moodi.member.infrastructure.persistence;

import com.moodi.member.domain.RefreshToken;
import com.moodi.member.domain.RefreshTokenRepository;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface RefreshTokenJpaRepository extends RefreshTokenRepository, Repository<RefreshToken, UUID> {
}
