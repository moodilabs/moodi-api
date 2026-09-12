package com.moodi.member.application.dto;

import com.moodi.member.domain.AgreementType;
import com.moodi.member.domain.Gender;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.shared.mood.MoodTag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MemberAdminDetail(
        UUID id,
        OAuthProvider provider,
        String email,
        String nickname,
        String country,
        Integer birthYear,
        Gender gender,
        MemberAdminStatus status,
        LocalDateTime createdAt,
        LocalDateTime deletedAt,
        LocalDateTime suspendedAt,
        String suspendReason,
        List<Agreement> agreements,
        List<MoodTag> preferredMoods,
        long savedSpotCount,
        long routeCount,
        long inquiryCount
) {

    public record Agreement(AgreementType type, boolean agreed, LocalDateTime agreedAt) {}
}
