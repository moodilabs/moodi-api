package com.moodi.spot.application.dto;

public record SpotSearchRow(
        Long spotId,
        String area,
        String district,
        long bookmarkCount,
        int matchRank
) {
}
