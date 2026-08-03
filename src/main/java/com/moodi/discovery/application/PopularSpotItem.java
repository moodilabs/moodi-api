package com.moodi.discovery.application;

public record PopularSpotItem(
        long spotId,
        String title,
        String imageUrl,
        String area,
        long bookmarkCount,
        boolean bookmarked
) {

    public static PopularSpotItem from(PopularSpotRow row) {
        return new PopularSpotItem(row.spotId(), row.title(), row.imageUrl(),
                row.area(), row.bookmarkCount(), row.bookmarked());
    }
}
