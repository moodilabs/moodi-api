package com.moodi.spot.application.dto;

import java.util.List;

public record PopularAreaSpotItem(
        Long spotId,
        String title,
        String imageUrl,
        List<String> moodTags
) {
}
