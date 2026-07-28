package com.moodi.spot.application.dto;

import java.util.List;

public record SpotDescriptionContext(
        Long spotId,
        String title,
        String contentType,
        String area,
        List<String> moodTags,
        String overview
) {

    public static SpotDescriptionContext from(SpotDetailSnapshot snapshot) {
        return new SpotDescriptionContext(
                snapshot.spotId(),
                snapshot.title(),
                snapshot.contentType().name(),
                snapshot.area(),
                snapshot.moodTags(),
                snapshot.overview()
        );
    }
}
