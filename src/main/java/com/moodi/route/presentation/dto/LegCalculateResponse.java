package com.moodi.route.presentation.dto;

import java.util.List;

public record LegCalculateResponse(
        List<LegInfo> legs
) {
    public record LegInfo(
            Long fromSpotId,
            Long toSpotId,
            String travelMode,
            int durationSeconds,
            int distanceMeters,
            String landingUrl
    ) {}
}
