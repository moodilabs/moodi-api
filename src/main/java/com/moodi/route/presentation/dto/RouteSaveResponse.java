package com.moodi.route.presentation.dto;

import com.moodi.route.application.RouteSaveResult;
import com.moodi.route.domain.TravelMode;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RouteSaveResponse(
        UUID publicId,
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
            String spotContentType,
            String spotDescription
    ) {
    }

    public record LegPlan(
            int fromSequence,
            int toSequence,
            String travelMode,
            int durationSeconds,
            int distanceMeters,
            String landingUrl
    ) {
    }

    public static RouteSaveResponse from(RouteSaveResult result) {
        List<DayPlan> dayPlans = result.days().stream()
                .map(RouteSaveResponse::toDayPlan)
                .toList();

        return new RouteSaveResponse(
                result.publicId(),
                result.title(),
                result.startDate(),
                result.endDate(),
                dayPlans
        );
    }

    private static DayPlan toDayPlan(RouteSaveResult.DayResult day) {
        List<SpotPlan> spots = day.spots().stream()
                .map(RouteSaveResponse::toSpotPlan)
                .toList();

        List<LegPlan> legs = day.legs().stream()
                .map(RouteSaveResponse::toLegPlan)
                .toList();

        return new DayPlan(day.dayNumber(), day.date(), spots, legs);
    }

    private static SpotPlan toSpotPlan(RouteSaveResult.SpotResult spot) {
        return new SpotPlan(
                spot.spotId(), spot.sequence(), spot.estimatedMinutes(),
                spot.title(), spot.imageUrl(),
                spot.area(), spot.district(),
                spot.latitude(), spot.longitude(),
                spot.contentType(), spot.description()
        );
    }

    private static LegPlan toLegPlan(RouteSaveResult.LegResult leg) {
        return new LegPlan(
                leg.fromSequence(), leg.toSequence(),
                leg.travelMode() != TravelMode.UNAVAILABLE ? leg.travelMode().name() : null,
                leg.durationSeconds(), leg.distanceMeters(),
                leg.landingUrl()
        );
    }
}
