package com.moodi.admin.application.dto;

import java.time.LocalDateTime;

public record AdminTokenPair(String accessToken, String refreshToken, LocalDateTime refreshTokenExpiresAt) {
}
