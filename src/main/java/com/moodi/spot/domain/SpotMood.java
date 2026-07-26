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
}
