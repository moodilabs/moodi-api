package com.moodi.spot.application.dto;

import java.util.List;

public record SimilarMoodSpotItem(
        Long spotId,
        String title,
        String imageUrl,
        String area,
        long bookmarkCount,
        List<String> moodTags
) {
}
