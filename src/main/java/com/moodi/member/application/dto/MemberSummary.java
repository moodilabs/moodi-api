package com.moodi.member.application.dto;

import com.moodi.member.domain.OAuthProvider;

public record MemberSummary(
        String nickname,
        String country,
        OAuthProvider provider,
        String email,
        long savedSpotCount,
        long routeCount
) {
}
