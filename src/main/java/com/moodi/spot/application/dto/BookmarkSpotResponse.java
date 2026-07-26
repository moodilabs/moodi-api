package com.moodi.spot.application.dto;

import com.moodi.shared.mood.MoodTag;

import java.time.LocalDateTime;
import java.util.List;

public record BookmarkSpotResponse(
        Long spotId,
        String title,
        String imageUrl,
        String area,
        List<MoodTag> moodTags,
        long bookmarkCount,
        LocalDateTime bookmarkedAt,
        boolean bookmarked
) {
}
