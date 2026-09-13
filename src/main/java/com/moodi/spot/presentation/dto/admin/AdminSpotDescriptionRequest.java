package com.moodi.spot.presentation.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSpotDescriptionRequest(
        @NotBlank(message = "설명은 필수입니다.")
        @Size(max = 2000, message = "설명은 2,000자 이하여야 합니다.")
        String content
) {
}
