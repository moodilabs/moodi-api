package com.moodi.member.presentation.dto;

import com.moodi.member.application.dto.ProfileCommand;
import com.moodi.member.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfileRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname,

        @NotBlank(message = "국가는 필수입니다.")
        String country,

        @NotNull(message = "출생연도는 필수입니다.")
        Integer birthYear,

        @NotNull(message = "성별은 필수입니다.")
        Gender gender
) {

    public ProfileCommand toCommand() {
        return new ProfileCommand(nickname, country, birthYear, gender);
    }
}
