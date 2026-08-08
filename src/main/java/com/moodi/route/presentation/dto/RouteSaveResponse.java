package com.moodi.route.presentation.dto;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteDay;
import com.moodi.route.domain.RouteLeg;
import com.moodi.route.domain.RouteSpot;
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

    public static RouteSaveResponse from(Route route) {
        List<DayPlan> dayPlans = route.getDays().stream()
                .map(RouteSaveResponse::toDayPlan)
                .toList();

        return new RouteSaveResponse(
                route.getPublicId(),
                route.getTitle(),
                route.getStartDate(),
                route.getEndDate(),
                dayPlans
        );
    }

    private static DayPlan toDayPlan(RouteDay day) {
        List<SpotPlan> spots = day.getSpots().stream()
                .map(RouteSaveResponse::toSpotPlan)
                .toList();

        List<LegPlan> legs = day.getLegs().stream()
                .map(RouteSaveResponse::toLegPlan)
                .toList();

        return new DayPlan(day.getDayNumber(), day.getDate(), spots, legs);
    }

    private static SpotPlan toSpotPlan(RouteSpot spot) {
        return new SpotPlan(
                spot.getSpotId(), spot.getSequence(), spot.getEstimatedMinutes(),
                spot.getSpotTitle(), spot.getSpotImageUrl(),
                spot.getSpotArea(), spot.getSpotDistrict(),
                spot.getSpotLatitude(), spot.getSpotLongitude(),
                spot.getSpotContentType(), spot.getSpotDescription()
        );
    }

    private static LegPlan toLegPlan(RouteLeg leg) {
        return new LegPlan(
                leg.getFromSequence(), leg.getToSequence(),
                leg.getTravelMode() != TravelMode.UNAVAILABLE ? leg.getTravelMode().name() : null,
                leg.getDurationSeconds(), leg.getDistanceMeters(),
                leg.getLandingUrl()
        );
    }
}
