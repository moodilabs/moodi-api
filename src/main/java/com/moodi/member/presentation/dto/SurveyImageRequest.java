package com.moodi.member.presentation.dto;

import com.moodi.member.application.SurveyImageService;
import com.moodi.shared.mood.MoodTag;

public record SurveyImageRequest(
        MoodTag mood,
        Long spotId,
        String imageUrl,
        int sortOrder
) {
    public SurveyImageService.Command toCommand() {
        return new SurveyImageService.Command(mood, spotId, imageUrl, sortOrder);
    }
}
