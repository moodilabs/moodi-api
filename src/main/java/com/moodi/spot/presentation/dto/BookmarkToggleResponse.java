package com.moodi.spot.presentation.dto;

import com.moodi.spot.application.dto.BookmarkToggleResult;

public record BookmarkToggleResponse(
        Long spotId,
        boolean bookmarked,
        long bookmarkCount
) {

    public static BookmarkToggleResponse from(BookmarkToggleResult result) {
        return new BookmarkToggleResponse(result.spotId(), result.bookmarked(), result.bookmarkCount());
    }
}
