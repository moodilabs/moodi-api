package com.moodi.member.application.dto;

public record LoginResult(String accessToken, String refreshToken, boolean isNewMember) {
}
