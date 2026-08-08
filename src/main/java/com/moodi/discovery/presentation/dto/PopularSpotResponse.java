package com.moodi.discovery.presentation.dto;

import com.moodi.discovery.application.PopularSpotItem;

public record PopularSpotResponse(
        long spotId,
        String title,
        String imageUrl,
        String area,
        long bookmarkCount,
        boolean bookmarked
) {

    public static PopularSpotResponse from(PopularSpotItem item) {
        return new PopularSpotResponse(item.spotId(), item.title(), item.imageUrl(),
                item.area(), item.bookmarkCount(), item.bookmarked());
    }
}
