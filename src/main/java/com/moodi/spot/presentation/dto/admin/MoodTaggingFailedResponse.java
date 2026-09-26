package com.moodi.spot.presentation.dto.admin;

import com.moodi.spot.domain.Spot;

import java.time.LocalDateTime;

public record MoodTaggingFailedResponse(
        Long spotId,
        String contentId,
        String area,
        int attemptCount,
        String lastError,
        LocalDateTime lastAttemptedAt
) {

    public static MoodTaggingFailedResponse from(Spot spot) {
        return new MoodTaggingFailedResponse(
                spot.getId(),
                spot.getContentId(),
                spot.getArea(),
                spot.getMoodTaggingAttemptCount(),
                spot.getMoodTaggingLastError(),
                spot.getMoodTaggingLastAttemptedAt()
        );
    }
}
