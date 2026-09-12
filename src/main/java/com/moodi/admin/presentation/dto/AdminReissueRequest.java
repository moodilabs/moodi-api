package com.moodi.admin.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminReissueRequest(
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}
