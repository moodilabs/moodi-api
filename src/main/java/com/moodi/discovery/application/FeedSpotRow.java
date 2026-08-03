package com.moodi.discovery.application;

/**
 * 피드 후보 1건. 정렬 키({@code seen}·{@code rankScore}·{@code shuffleKey}·{@code spotId})를
 * 함께 실어 커서 발급에 쓴다.
 */
public record FeedSpotRow(
        long spotId,
        String title,
        String imageUrl,
        String area,
        boolean bookmarked,
        int seen,
        long rankScore,
        String shuffleKey
) {
}
