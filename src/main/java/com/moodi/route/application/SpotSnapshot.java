package com.moodi.route.application;

import com.moodi.spot.domain.SpotContentType;

public record SpotSnapshot(
        Long spotId,
        String title,
        String imageUrl,
        String area,
        String district,
        Double latitude,
        Double longitude,
        SpotContentType contentType,
        String description
) {
}
