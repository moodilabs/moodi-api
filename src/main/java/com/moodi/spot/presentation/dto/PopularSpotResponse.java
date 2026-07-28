package com.moodi.spot.presentation.dto;

import java.util.List;

public record PopularSpotResponse(
        Long spotId,
        String title,
        String imageUrl,
        List<String> moodTags
) {
}
