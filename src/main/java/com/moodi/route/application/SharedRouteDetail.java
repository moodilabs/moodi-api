package com.moodi.route.application;

import com.moodi.route.domain.Route;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SharedRouteDetail(
        UUID publicId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        boolean isOwner,
        List<RouteDetail.DayDetail> days
) {

    public static SharedRouteDetail from(Route route, boolean isOwner) {
        RouteDetail detail = RouteDetail.from(route);
        return new SharedRouteDetail(
                detail.publicId(),
                detail.title(),
                detail.startDate(),
                detail.endDate(),
                detail.totalDays(),
                isOwner,
                detail.days()
        );
    }
}
