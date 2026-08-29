package com.moodi.spot.application.dto;

import com.moodi.shared.mood.MoodTag;

import java.util.List;

public record SpotSearchItem(
        Long spotId,
        String title,
        String imageUrl,
        String area,
        String district,
        String description,
        List<MoodTag> moodTags,
        long bookmarkCount,
        boolean bookmarked,
        boolean inRoute
) {
}
