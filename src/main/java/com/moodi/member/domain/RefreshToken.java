package com.moodi.member.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    private UUID id;
    private UUID memberId;
    private String token;
    private LocalDateTime expiresAt;

    private RefreshToken(UUID memberId, String token, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken issue(UUID memberId, String token, LocalDateTime expiresAt) {
        return new RefreshToken(memberId, token, expiresAt);
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }
}
