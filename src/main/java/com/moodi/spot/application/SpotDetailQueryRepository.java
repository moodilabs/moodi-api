package com.moodi.spot.application;

import com.moodi.spot.presentation.dto.PopularSpotResponse;
import com.moodi.spot.presentation.dto.SimilarMoodSpotResponse;

import java.util.List;

public interface SpotDetailQueryRepository {

    List<SimilarMoodSpotResponse> findSimilarMoodSpots(Long spotId, List<String> moodTagKeys, int limit);

    List<PopularSpotResponse> findPopularSpotsByArea(Long spotId, String area, String district, int limit);
}
