package com.moodi.spot.support;

import com.moodi.spot.application.dto.SpotSearchRow;

public class SpotSearchRowFixture {

    private static final Long DEFAULT_SPOT_ID = 8317L;
    private static final String DEFAULT_AREA = "부산";
    private static final String DEFAULT_DISTRICT = "남구";
    private static final long DEFAULT_BOOKMARK_COUNT = 5L;
    private static final int DEFAULT_MATCH_RANK = 0;

    public static SpotSearchRow create() {
        return new SpotSearchRow(
                DEFAULT_SPOT_ID, DEFAULT_AREA, DEFAULT_DISTRICT,
                DEFAULT_BOOKMARK_COUNT, DEFAULT_MATCH_RANK);
    }

    public static SpotSearchRow create(Long spotId) {
        return new SpotSearchRow(
                spotId, DEFAULT_AREA, DEFAULT_DISTRICT,
                DEFAULT_BOOKMARK_COUNT, DEFAULT_MATCH_RANK);
    }

    public static SpotSearchRow create(Long spotId, String district, long bookmarkCount) {
        return new SpotSearchRow(
                spotId, DEFAULT_AREA, district, bookmarkCount, DEFAULT_MATCH_RANK);
    }

    public static SpotSearchRow create(Long spotId, String area, String district, long bookmarkCount) {
        return new SpotSearchRow(
                spotId, area, district, bookmarkCount, DEFAULT_MATCH_RANK);
    }

    public static SpotSearchRow create(Long spotId, String district, long bookmarkCount, int matchRank) {
        return new SpotSearchRow(
                spotId, DEFAULT_AREA, district, bookmarkCount, matchRank);
    }

    private SpotSearchRowFixture() {
    }
}
