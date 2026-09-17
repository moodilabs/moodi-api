package com.moodi.spot.presentation.dto;

import java.util.List;

public record SimilarMoodSpotResponse(
        Long spotId,
        String title,
        String imageUrl,
        String area,
        long bookmarkCount,
        List<String> moodTags
) {
}
