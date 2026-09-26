package com.moodi.member.presentation.dto;

import com.moodi.member.application.SurveyImageService;
import com.moodi.shared.mood.MoodTag;

public record SurveyImageResponse(
        Long id,
        MoodTag mood,
        Long spotId,
        String imageUrl,
        int sortOrder
) {
    public static SurveyImageResponse from(SurveyImageService.View view) {
        return new SurveyImageResponse(
                view.id(), view.mood(), view.spotId(),
                view.imageUrl(), view.sortOrder()
        );
    }
}
