package com.moodi.route.application;

import com.moodi.route.domain.RouteSpotType;

import java.util.List;

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
        List<String> moodTagKeys,
        boolean required
) {
    public SpotSnapshot(Long spotId, String title, String imageUrl,
                        String area, String district,
                        Double latitude, Double longitude,
                        RouteSpotType contentType, String description,
                        List<String> moodTagKeys) {
        this(spotId, title, imageUrl, area, district, latitude, longitude,
                contentType, description, moodTagKeys, true);
    }

    public SpotSnapshot asOptional() {
        return new SpotSnapshot(spotId, title, imageUrl, area, district,
                latitude, longitude, contentType, description, moodTagKeys, false);
    }
}
