package com.moodi.member.application.dto;

import java.time.LocalDateTime;

public record TokenPair(String accessToken, String refreshToken, LocalDateTime refreshTokenExpiresAt) {
}
