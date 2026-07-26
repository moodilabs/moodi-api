package com.moodi.spot.application.dto;

import java.time.LocalDateTime;

public record BookmarkSpotRow(
        Long bookmarkId,
        Long spotId,
        String area,
        LocalDateTime bookmarkedAt,
        long bookmarkCount
) {
}
