package com.moodi.member.presentation.dto;

import com.moodi.shared.mood.MoodTag;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PreferredMoodRequest(
        @NotNull(message = "선호 무드 목록은 필수입니다.")
        List<@NotNull(message = "선호 무드 값은 비어 있을 수 없습니다.") MoodTag> moods
) {
}
