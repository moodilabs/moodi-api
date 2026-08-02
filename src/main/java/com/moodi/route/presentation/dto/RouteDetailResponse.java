package com.moodi.route.presentation.dto;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteDay;
import com.moodi.route.domain.RouteLeg;
import com.moodi.route.domain.RouteSpot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RouteDetailResponse(
        UUID publicId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        List<DayDetail> days
) {
    public record DayDetail(
            int dayNumber,
            LocalDate date,
            List<SpotDetail> spots,
            List<LegDetail> legs
    ) {
    }

    public record SpotDetail(
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

    public record LegDetail(
            int fromSequence,
            int toSequence,
            String travelMode,
            int durationSeconds,
            int distanceMeters,
            String landingUrl
    ) {
    }

    public static RouteDetailResponse from(Route route) {
        List<DayDetail> dayDetails = route.getDays().stream()
                .map(RouteDetailResponse::toDayDetail)
                .toList();

        return new RouteDetailResponse(
                route.getPublicId(),
                route.getTitle(),
                route.getStartDate(),
                route.getEndDate(),
                route.getTotalDays(),
                dayDetails
        );
    }

    private static DayDetail toDayDetail(RouteDay day) {
        List<SpotDetail> spots = day.getSpots().stream()
                .map(RouteDetailResponse::toSpotDetail)
                .toList();

        List<LegDetail> legs = day.getLegs().stream()
                .map(RouteDetailResponse::toLegDetail)
                .toList();

        return new DayDetail(day.getDayNumber(), day.getDate(), spots, legs);
    }

    private static SpotDetail toSpotDetail(RouteSpot spot) {
        return new SpotDetail(
                spot.getSpotId(), spot.getSequence(), spot.getEstimatedMinutes(),
                spot.getSpotTitle(), spot.getSpotImageUrl(),
                spot.getSpotArea(), spot.getSpotDistrict(),
                spot.getSpotLatitude(), spot.getSpotLongitude(),
                spot.getSpotContentType(), spot.getSpotDescription()
        );
    }

    private static LegDetail toLegDetail(RouteLeg leg) {
        return new LegDetail(
                leg.getFromSequence(), leg.getToSequence(),
                leg.getTravelMode() != null ? leg.getTravelMode().name() : null,
                leg.getDurationSeconds(), leg.getDistanceMeters(),
                leg.getLandingUrl()
        );
    }
}
