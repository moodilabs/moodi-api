package com.moodi.spot.presentation.dto.admin;

import jakarta.validation.constraints.NotNull;

public record AdminSpotRouteExclusionRequest(
        @NotNull(message = "제외 여부는 필수입니다.")
        Boolean excluded
) {
}
