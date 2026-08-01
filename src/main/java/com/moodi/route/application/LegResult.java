package com.moodi.route.application;

import com.moodi.route.domain.TravelMode;

public record LegResult(
        TravelMode travelMode,
        int durationSeconds,
        int distanceMeters,
        String landingUrl
) {
}
