package com.moodi.spot.application.dto;

public record SpotImageItem(
        String imageUrl,
        boolean isPrimary,
        int sortOrder
) {
}
