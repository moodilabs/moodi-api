package com.moodi.route.application;

import com.moodi.route.domain.RouteSpotType;

public record SpotSnapshot(
        Long spotId,
        String title,
        String imageUrl,
        String area,
        String district,
        Double latitude,
        Double longitude,
        RouteSpotType contentType,
        String description
) {
}
