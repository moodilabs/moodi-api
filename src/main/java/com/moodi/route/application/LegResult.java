package com.moodi.route.application;

import com.moodi.route.domain.TravelMode;

public record LegResult(
        TravelMode travelMode,
        int durationSeconds,
        int distanceMeters,
        String landingUrl
) {

    public static LegResult unavailable() {
        return new LegResult(TravelMode.UNAVAILABLE, 0, 0, null);
    }

    public boolean isAvailable() {
        return travelMode != TravelMode.UNAVAILABLE;
    }
}
