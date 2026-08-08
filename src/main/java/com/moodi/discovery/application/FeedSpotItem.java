package com.moodi.discovery.application;

/**
 * 피드 List Item (FED-F01). 비회원이면 {@code bookmarked}는 항상 false다.
 */
public record FeedSpotItem(
        long spotId,
        String title,
        String imageUrl,
        String area,
        boolean bookmarked
) {

    public static FeedSpotItem from(FeedSpotRow row) {
        return new FeedSpotItem(row.spotId(), row.title(), row.imageUrl(), row.area(), row.bookmarked());
    }
}
