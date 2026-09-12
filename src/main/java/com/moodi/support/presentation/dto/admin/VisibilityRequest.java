package com.moodi.support.presentation.dto.admin;

import jakarta.validation.constraints.NotNull;

public record VisibilityRequest(
        @NotNull(message = "노출 여부는 필수입니다.")
        Boolean visible
) {
}
