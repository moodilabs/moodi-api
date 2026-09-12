package com.moodi.admin.infrastructure.persistence;

import com.moodi.admin.domain.AdminRefreshToken;
import com.moodi.admin.domain.AdminRefreshTokenRepository;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface AdminRefreshTokenJpaRepository extends AdminRefreshTokenRepository,
        Repository<AdminRefreshToken, UUID> {
}
