package com.moodi.route.presentation.dto;

import java.time.LocalDate;
import java.util.List;

public record RouteGenerateResponse(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<DayPlan> days
) {
    public record DayPlan(
            int dayNumber,
            LocalDate date,
            List<SpotPlan> spots,
            List<LegPlan> legs
    ) {
    }

    public record SpotPlan(
            Long spotId,
            int sequence,
            int estimatedMinutes,
            String spotTitle,
            String spotImageUrl,
            String spotArea,
            String spotDistrict,
            Double spotLatitude,
            Double spotLongitude,
            String spotContentType
    ) {
    }

    public record LegPlan(
            int fromSequence,
            int toSequence,
            String travelMode,
            int durationSeconds,
            int distanceMeters
    ) {
    }
}
