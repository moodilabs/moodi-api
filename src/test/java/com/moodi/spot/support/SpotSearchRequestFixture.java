package com.moodi.spot.support;

import com.moodi.spot.application.dto.SpotSearchRequest;
import com.moodi.spot.application.dto.SpotSearchSortType;

import java.util.UUID;

public class SpotSearchRequestFixture {

    public static SpotSearchRequest createMostSaved() {
        return SpotSearchRequest.of(
                null, null, null, false, SpotSearchSortType.MOST_SAVED, null, null, 20);
    }

    public static SpotSearchRequest createMostSaved(int size) {
        return SpotSearchRequest.of(
                null, null, null, false, SpotSearchSortType.MOST_SAVED, null, null, size);
    }

    public static SpotSearchRequest createMostSaved(UUID routePublicId) {
        return SpotSearchRequest.of(
                null, null, null, false, SpotSearchSortType.MOST_SAVED, routePublicId, null, 20);
    }

    public static SpotSearchRequest createBestMatch(String keyword) {
        return SpotSearchRequest.of(
                keyword, null, null, false, SpotSearchSortType.BEST_MATCH, null, null, 20);
    }

    private SpotSearchRequestFixture() {
    }
}
