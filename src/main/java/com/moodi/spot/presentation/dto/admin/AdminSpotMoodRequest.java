package com.moodi.spot.presentation.dto.admin;

import com.moodi.shared.mood.MoodTag;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdminSpotMoodRequest(
        @NotEmpty(message = "무드 태그를 1개 이상 지정해주세요.")
        List<MoodTag> moodTags
) {
}
