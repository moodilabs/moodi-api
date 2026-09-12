package com.moodi.member.presentation.dto;

import com.moodi.member.application.dto.MemberSummary;
import com.moodi.member.domain.OAuthProvider;

public record MemberSummaryResponse(
        String nickname,
        String country,
        OAuthProvider provider,
        String email,
        long savedSpotCount,
        long routeCount
) {

    public static MemberSummaryResponse from(MemberSummary summary) {
        return new MemberSummaryResponse(
                summary.nickname(),
                summary.country(),
                summary.provider(),
                summary.email(),
                summary.savedSpotCount(),
                summary.routeCount()
        );
    }
}
