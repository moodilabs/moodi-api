package com.moodi.spot.application.dto;

import java.time.LocalDateTime;

public record BookmarkSpotRow(
        Long bookmarkId,
        Long spotId,
        String area,
        String district,
        Double latitude,
        Double longitude,
        LocalDateTime bookmarkedAt,
        long bookmarkCount
) {
}
