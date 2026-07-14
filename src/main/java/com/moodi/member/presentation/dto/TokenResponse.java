package com.moodi.member.presentation.dto;

import com.moodi.member.application.dto.LoginResult;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        boolean isNewMember
) {

    private static final String BEARER = "Bearer";

    public static TokenResponse from(LoginResult result) {
        return new TokenResponse(result.accessToken(), result.refreshToken(), BEARER, result.isNewMember());
    }
}
