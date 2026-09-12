package com.moodi.admin.domain;

import java.util.Optional;
import java.util.UUID;

public interface AdminRefreshTokenRepository {

    AdminRefreshToken save(AdminRefreshToken token);

    Optional<AdminRefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByAdminId(UUID adminId);
}
