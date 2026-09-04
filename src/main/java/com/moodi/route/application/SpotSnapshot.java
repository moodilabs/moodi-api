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
        String description,
        boolean required
) {
    public SpotSnapshot(Long spotId, String title, String imageUrl,
                        String area, String district,
                        Double latitude, Double longitude,
                        RouteSpotType contentType, String description) {
        this(spotId, title, imageUrl, area, district, latitude, longitude,
                contentType, description, true);
    }

    public SpotSnapshot asOptional() {
        return new SpotSnapshot(spotId, title, imageUrl, area, district,
                latitude, longitude, contentType, description, false);
    }
}
