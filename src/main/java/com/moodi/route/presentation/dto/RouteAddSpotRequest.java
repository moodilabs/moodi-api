package com.moodi.route.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record RouteAddSpotRequest(
        @NotNull Long spotId
) {
}
