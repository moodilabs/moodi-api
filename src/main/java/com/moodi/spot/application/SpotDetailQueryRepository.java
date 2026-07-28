package com.moodi.spot.application;

import com.moodi.spot.application.dto.PopularAreaSpotItem;
import com.moodi.spot.application.dto.SimilarMoodSpotItem;

import java.util.List;

public interface SpotDetailQueryRepository {

    List<SimilarMoodSpotItem> findSimilarMoodSpots(Long spotId, List<String> moodTagKeys, int limit);

    List<PopularAreaSpotItem> findPopularSpotsByArea(Long spotId, String area, String district, int limit);
}
