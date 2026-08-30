package com.moodi.spot.presentation.dto;

import com.moodi.shared.mood.MoodTag;
import com.moodi.spot.application.dto.BookmarkSpotItem;

import java.time.LocalDateTime;
import java.util.List;

public record BookmarkSpotResponse(
        Long spotId,
        String title,
        String imageUrl,
        String area,
        String district,
        String description,
        Double latitude,
        Double longitude,
        List<MoodTag> moodTags,
        long bookmarkCount,
        LocalDateTime bookmarkedAt,
        boolean bookmarked
) {

    public static BookmarkSpotResponse from(BookmarkSpotItem item) {
        return new BookmarkSpotResponse(
                item.spotId(), item.title(), item.imageUrl(),
                item.area(), item.district(), item.description(),
                item.latitude(), item.longitude(),
                item.moodTags(), item.bookmarkCount(), item.bookmarkedAt(), item.bookmarked()
        );
    }
}
