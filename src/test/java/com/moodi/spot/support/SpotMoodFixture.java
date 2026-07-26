package com.moodi.spot.support;

import com.moodi.shared.mood.MoodTag;
import com.moodi.spot.domain.SpotMood;

import java.util.List;

public class SpotMoodFixture {

    public static SpotMood create(Long spotId) {
        return SpotMood.create(spotId, MoodVectorFixture.create(),
                List.of(MoodTag.COZY, MoodTag.SERENE), 0.85);
    }

    public static SpotMood create(Long spotId, List<MoodTag> tags) {
        return SpotMood.create(spotId, MoodVectorFixture.create(), tags, 0.85);
    }

    private SpotMoodFixture() {
    }
}
