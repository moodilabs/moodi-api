package com.moodi.member.presentation.dto;

import com.moodi.member.domain.OAuthProvider;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "provider는 필수입니다") String provider,
        @NotBlank(message = "idToken은 필수입니다") String idToken
) {

    public OAuthProvider toProvider() {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
