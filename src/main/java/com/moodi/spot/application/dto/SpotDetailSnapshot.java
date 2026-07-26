package com.moodi.spot.application.dto;

import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.presentation.dto.PopularSpotResponse;
import com.moodi.spot.presentation.dto.SimilarMoodSpotResponse;
import com.moodi.spot.presentation.dto.SpotDetailResponse.SpotImageResponse;

import java.util.List;

public record SpotDetailSnapshot(
        Long spotId,
        String title,
        String area,
        String district,
        String overview,
        String homepage,
        String tel,
        SpotContentType contentType,
        List<String> moodTags,
        List<String> moodTagKeys,
        List<SpotImageResponse> images,
        long bookmarkCount,
        boolean bookmarked,
        Double latitude,
        Double longitude,
        String addr1,
        String addr2,
        List<SimilarMoodSpotResponse> similarMoodSpots,
        List<PopularSpotResponse> popularAreaSpots
) {
}
