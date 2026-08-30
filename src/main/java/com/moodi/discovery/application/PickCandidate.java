package com.moodi.discovery.application;

import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.mood.MoodVector;

import java.util.List;

/**
 * 추천 후보 스팟 1건. 다른 컨텍스트 타입을 그대로 쓰지 않도록 discovery 자신의 타입으로 정의한다.
 */
public record PickCandidate(
        long spotId,
        String title,
        String imageUrl,
        String area,
        String district,
        String neighborhood,
        String address,
        String description,
        Double latitude,
        Double longitude,
        List<MoodTag> moodTags,
        MoodVector moodVector,
        boolean bookmarked
) {
}
