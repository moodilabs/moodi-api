package com.moodi.route.application;

import java.time.LocalDate;
import java.util.List;

public record RouteGenerateResult(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<DayResult> days
) {
    public record DayResult(
            int dayNumber,
            LocalDate date,
            List<SpotResult> spots,
            List<LegResultItem> legs
    ) {
    }

    public record SpotResult(
            Long spotId,
            int sequence,
            int estimatedMinutes,
            String spotTitle,
            String spotImageUrl,
            String spotArea,
            String spotDistrict,
            Double spotLatitude,
            Double spotLongitude,
            String spotContentType,
            String spotDescription
    ) {
    }

    public record LegResultItem(
            int fromSequence,
            int toSequence,
            String travelMode,
            int durationSeconds,
            int distanceMeters,
            String landingUrl
    ) {
    }
}
