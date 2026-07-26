package com.moodi.spot.application.dto;

public record BookmarkToggleResponse(
        Long spotId,
        boolean bookmarked,
        long bookmarkCount
) {
}
