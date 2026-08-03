package com.moodi.discovery.application;

public record PopularSpotRow(
        long spotId,
        String title,
        String imageUrl,
        String area,
        long bookmarkCount,
        boolean bookmarked
) {
}
