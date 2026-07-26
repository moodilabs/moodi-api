package com.moodi.spot.application.dto;

public record BookmarkToggleResult(
        Long spotId,
        boolean bookmarked,
        long bookmarkCount
) {
}
