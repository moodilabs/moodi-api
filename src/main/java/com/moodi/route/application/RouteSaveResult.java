package com.moodi.route.application;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteDay;
import com.moodi.route.domain.RouteLeg;
import com.moodi.route.domain.RouteSpot;
import com.moodi.route.domain.TravelMode;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RouteSaveResult(
        UUID publicId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<DayResult> days
) {
    public record DayResult(
            int dayNumber,
            LocalDate date,
            List<SpotResult> spots,
            List<LegResult> legs
    ) {}

    public record SpotResult(
            Long spotId,
            int sequence,
            int estimatedMinutes,
            String title,
            String imageUrl,
            String area,
            String district,
            Double latitude,
            Double longitude,
            String contentType,
            String description
    ) {}

    public record LegResult(
            int fromSequence,
            int toSequence,
            TravelMode travelMode,
            int durationSeconds,
            int distanceMeters,
            String landingUrl
    ) {}

    public static RouteSaveResult from(Route route) {
        List<DayResult> dayResults = route.getDays().stream()
                .map(RouteSaveResult::toDayResult)
                .toList();

        return new RouteSaveResult(
                route.getPublicId(),
                route.getTitle(),
                route.getStartDate(),
                route.getEndDate(),
                dayResults
        );
    }

    private static DayResult toDayResult(RouteDay day) {
        List<SpotResult> spots = day.getSpots().stream()
                .map(RouteSaveResult::toSpotResult)
                .toList();

        List<LegResult> legs = day.getLegs().stream()
                .map(RouteSaveResult::toLegResult)
                .toList();

        return new DayResult(day.getDayNumber(), day.getDate(), spots, legs);
    }

    private static SpotResult toSpotResult(RouteSpot spot) {
        return new SpotResult(
                spot.getSpotId(), spot.getSequence(), spot.getEstimatedMinutes(),
                spot.getSpotTitle(), spot.getSpotImageUrl(),
                spot.getSpotArea(), spot.getSpotDistrict(),
                spot.getSpotLatitude(), spot.getSpotLongitude(),
                spot.getSpotContentType(), spot.getSpotDescription()
        );
    }

    private static LegResult toLegResult(RouteLeg leg) {
        return new LegResult(
                leg.getFromSequence(), leg.getToSequence(),
                leg.getTravelMode(),
                leg.getDurationSeconds(), leg.getDistanceMeters(),
                leg.getLandingUrl()
        );
    }
}
