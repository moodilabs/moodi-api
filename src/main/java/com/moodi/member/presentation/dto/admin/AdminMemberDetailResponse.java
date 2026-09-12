package com.moodi.member.presentation.dto.admin;

import com.moodi.member.application.dto.MemberAdminDetail;
import com.moodi.member.application.dto.MemberAdminStatus;
import com.moodi.member.domain.AgreementType;
import com.moodi.member.domain.Gender;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.shared.mood.MoodTag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminMemberDetailResponse(
        UUID id, OAuthProvider provider, String email, String nickname, String country, Integer birthYear,
        Gender gender, MemberAdminStatus status, LocalDateTime createdAt, LocalDateTime deletedAt,
        LocalDateTime suspendedAt, String suspendReason, List<AgreementResponse> agreements,
        List<MoodTag> preferredMoods, long savedSpotCount, long routeCount, long inquiryCount
) {

    public record AgreementResponse(AgreementType type, boolean agreed, LocalDateTime agreedAt) {}

    public static AdminMemberDetailResponse from(MemberAdminDetail detail) {
        return new AdminMemberDetailResponse(detail.id(), detail.provider(), detail.email(), detail.nickname(),
                detail.country(), detail.birthYear(), detail.gender(), detail.status(), detail.createdAt(),
                detail.deletedAt(), detail.suspendedAt(), detail.suspendReason(),
                detail.agreements().stream()
                        .map(agreement -> new AgreementResponse(agreement.type(), agreement.agreed(), agreement.agreedAt()))
                        .toList(),
                detail.preferredMoods(), detail.savedSpotCount(), detail.routeCount(), detail.inquiryCount());
    }
}
