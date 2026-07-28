package com.moodi.spot.application.dto;

public record SimilarMoodSpotItem(
        Long spotId,
        String title,
        String imageUrl,
        String area,
        long bookmarkCount
) {
}
