package com.moodi.discovery.presentation.dto;

import com.moodi.discovery.application.FeedSpotItem;

public record FeedSpotResponse(
        long spotId,
        String title,
        String imageUrl,
        String area,
        boolean bookmarked
) {

    public static FeedSpotResponse from(FeedSpotItem item) {
        return new FeedSpotResponse(item.spotId(), item.title(), item.imageUrl(),
                item.area(), item.bookmarked());
    }
}
