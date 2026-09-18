package com.moodi.member.presentation.dto;

import com.moodi.member.domain.OAuthProvider;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "provider는 필수입니다") String provider,
        @NotBlank(message = "idToken은 필수입니다") String idToken,
        /** Apple 인가 코드(선택). 5분·1회용이라 로그인 요청에 바로 실어 보낸다. 탈퇴 시 계정 연결 철회에 필요. */
        String authorizationCode
) {

    public LoginRequest(String provider, String idToken) {
        this(provider, idToken, null);
    }

    public OAuthProvider toProvider() {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
