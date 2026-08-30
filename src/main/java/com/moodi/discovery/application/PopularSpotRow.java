package com.moodi.discovery.application;

public record PopularSpotRow(
        long spotId,
        String title,
        String imageUrl,
        String area,
        String description,
        long bookmarkCount,
        boolean bookmarked
) {
}
