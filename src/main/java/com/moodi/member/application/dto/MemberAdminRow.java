package com.moodi.member.application.dto;

import com.moodi.member.domain.OAuthProvider;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberAdminRow(UUID id, OAuthProvider provider, String email, String nickname, String country,
                             MemberAdminStatus status, LocalDateTime createdAt, LocalDateTime deletedAt,
                             LocalDateTime suspendedAt) {
}
