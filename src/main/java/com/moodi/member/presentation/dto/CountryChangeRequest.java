package com.moodi.member.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CountryChangeRequest(
        @NotBlank(message = "국가는 필수입니다.")
        String country
) {
}
