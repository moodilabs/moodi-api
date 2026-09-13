package com.moodi.spot.domain;

import java.util.List;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.mood.MoodVector;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotMood extends BaseEntity {

    private Long id;
    private Long spotId;
    private MoodVector moodVector;
    private List<MoodTag> moodTags;
    private Double confidence;

    private SpotMood(Long spotId, MoodVector moodVector, List<MoodTag> moodTags, Double confidence) {
        this.spotId = spotId;
        this.moodVector = moodVector;
        this.moodTags = List.copyOf(moodTags);
        this.confidence = confidence;
    }

    public static SpotMood create(Long spotId, MoodVector moodVector, List<MoodTag> moodTags, Double confidence) {
        return new SpotMood(spotId, moodVector, moodTags, confidence);
    }

    /**
     * 어드민 수동 보정. 태그만 바꾸고 무드 벡터는 유지한다 — 벡터는 유사도 계산용이고 태그는 표시·필터용이라 독립적이다.
     * 사람이 정한 값이므로 confidence는 1.0으로 올린다.
     */
    public void overrideTags(List<MoodTag> moodTags) {
        if (moodTags == null || moodTags.isEmpty()) {
            throw new com.moodi.shared.error.BusinessException(com.moodi.shared.error.ErrorCode.INVALID_REQUEST);
        }
        this.moodTags = List.copyOf(new java.util.LinkedHashSet<>(moodTags));
        this.confidence = 1.0;
    }
}
