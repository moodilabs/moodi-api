package com.moodi.member.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record NicknameChangeRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname
) {
}
