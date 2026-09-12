package com.moodi.member.presentation.dto.admin;

import com.moodi.member.application.dto.MemberAdminRow;
import com.moodi.member.application.dto.MemberAdminStatus;
import com.moodi.member.domain.OAuthProvider;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminMemberSummaryResponse(UUID id, OAuthProvider provider, String email, String nickname,
                                         String country, MemberAdminStatus status, LocalDateTime createdAt,
                                         LocalDateTime deletedAt, LocalDateTime suspendedAt) {

    public static AdminMemberSummaryResponse from(MemberAdminRow row) {
        return new AdminMemberSummaryResponse(row.id(), row.provider(), row.email(), row.nickname(), row.country(),
                row.status(), row.createdAt(), row.deletedAt(), row.suspendedAt());
    }
}
