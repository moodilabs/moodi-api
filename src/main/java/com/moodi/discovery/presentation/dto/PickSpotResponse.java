package com.moodi.discovery.presentation.dto;

import com.moodi.discovery.application.PickResultItem;
import com.moodi.shared.mood.MoodTag;

import java.util.List;

public record PickSpotResponse(
        long spotId,
        String title,
        String imageUrl,
        String area,
        String address,
        String description,
        Double latitude,
        Double longitude,
        List<String> moodTags,
        boolean bookmarked
) {

    public static PickSpotResponse from(PickResultItem item) {
        return new PickSpotResponse(
                item.spotId(),
                item.title(),
                item.imageUrl(),
                item.area(),
                item.address(),
                item.description(),
                item.latitude(),
                item.longitude(),
                item.moodTags().stream().map(MoodTag::getDisplayTag).toList(),
                item.bookmarked()
        );
    }
}
