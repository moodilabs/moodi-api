package com.moodi.spot.presentation.dto;

import com.moodi.shared.mood.MoodTag;

import java.util.List;

public record SpotDetailResponse(
        Long spotId,
        String title,
        String area,
        String district,
        String overview,
        String homepage,
        String tel,
        List<String> moodTags,
        List<SpotImageResponse> images,
        String aiDescription,
        long bookmarkCount,
        boolean bookmarked,
        Double latitude,
        Double longitude,
        String addr1,
        String addr2,
        List<SimilarMoodSpotResponse> similarMoodSpots,
        List<PopularSpotResponse> popularAreaSpots
) {

    public record SpotImageResponse(
            String imageUrl,
            boolean isPrimary,
            int sortOrder
    ) {
    }
}
