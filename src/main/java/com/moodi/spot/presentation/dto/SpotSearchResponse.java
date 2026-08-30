package com.moodi.spot.presentation.dto;

import com.moodi.shared.mood.MoodTag;
import com.moodi.spot.application.dto.SpotSearchItem;

import java.util.List;

public record SpotSearchResponse(
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

    public static SpotSearchResponse from(SpotSearchItem item) {
        return new SpotSearchResponse(
                item.spotId(), item.title(), item.imageUrl(),
                item.area(), item.district(), item.description(),
                item.moodTags(), item.bookmarkCount(),
                item.bookmarked(), item.inRoute()
        );
    }
}
