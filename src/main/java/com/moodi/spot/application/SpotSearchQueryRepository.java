package com.moodi.spot.application;

import com.moodi.spot.application.dto.SpotSearchRow;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SpotSearchQueryRepository {

    List<SpotSearchRow> searchByBestMatch(String keyword, String area, List<String> moodTagKeys,
                                           boolean saved, UUID memberId,
                                           Integer cursorMatchRank, Long cursorBookmarkCount,
                                           Long cursorSpotId, int size);

    List<SpotSearchRow> searchByMostSaved(String area, List<String> moodTagKeys,
                                           boolean saved, UUID memberId,
                                           Long cursorSpotId, Long cursorBookmarkCount, int size);

    Set<Long> findSpotIdsInRoute(UUID routePublicId, List<Long> spotIds);
}
