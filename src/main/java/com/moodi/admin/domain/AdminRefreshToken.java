package com.moodi.admin.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminRefreshToken extends BaseEntity {

    private UUID id;
    private UUID adminId;
    private String token;
    private LocalDateTime expiresAt;

    private AdminRefreshToken(UUID adminId, String token, LocalDateTime expiresAt) {
        this.adminId = adminId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public static AdminRefreshToken issue(UUID adminId, String token, LocalDateTime expiresAt) {
        return new AdminRefreshToken(adminId, token, expiresAt);
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }
}
