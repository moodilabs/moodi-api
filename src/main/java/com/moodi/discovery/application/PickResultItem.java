package com.moodi.discovery.application;

import com.moodi.shared.mood.MoodTag;

import java.util.List;

public record PickResultItem(
        long spotId,
        String title,
        String imageUrl,
        String area,
        String address,
        String description,
        Double latitude,
        Double longitude,
        List<MoodTag> moodTags,
        boolean bookmarked
) {

    private static final int MAX_MOOD_TAGS = 3;

    public static PickResultItem from(PickCandidate candidate) {
        return new PickResultItem(
                candidate.spotId(),
                candidate.title(),
                candidate.imageUrl(),
                candidate.area(),
                candidate.address(),
                candidate.description(),
                candidate.latitude(),
                candidate.longitude(),
                candidate.moodTags().stream().limit(MAX_MOOD_TAGS).toList(),
                candidate.bookmarked()
        );
    }
}
