package com.moodi.route.application;

import com.moodi.route.domain.Route;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @param shortCode 단축 링크 코드. 단축 코드 도입 전에 공유된 루트는 소유자가 다시 공유하기 전까지 null
 * @param shareUrl  밖으로 내보낼 공유 링크. 컨트롤러가 요청 호스트로 채운다({@link #withShareUrl})
 */
public record SharedRouteDetail(
        UUID publicId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        boolean isOwner,
        String shortCode,
        String shareUrl,
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
                route.getShortCode(),
                null,
                detail.days()
        );
    }

    public SharedRouteDetail withShareUrl(String shareUrl) {
        return new SharedRouteDetail(publicId, title, startDate, endDate, totalDays, isOwner, shortCode, shareUrl, days);
    }
}
