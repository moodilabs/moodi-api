package com.moodi.spot.presentation.dto;

public record SimilarMoodSpotResponse(
        Long spotId,
        String title,
        String imageUrl,
        String area,
        long bookmarkCount
) {
}
