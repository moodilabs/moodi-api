package com.moodi.spot.presentation.dto.admin;

import com.moodi.spot.application.dto.SpotAdminRow;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotStatus;

import java.time.LocalDateTime;

public record AdminSpotSummaryResponse(Long id, String title, SpotContentType contentType, String area,
                                       String district, SpotStatus status, boolean routeExcluded,
                                       long bookmarkCount, LocalDateTime createdAt, LocalDateTime statusChangedAt) {

    public static AdminSpotSummaryResponse from(SpotAdminRow row) {
        return new AdminSpotSummaryResponse(row.id(), row.title(), row.contentType(), row.area(), row.district(),
                row.status(), row.routeExcluded(), row.bookmarkCount(), row.createdAt(), row.statusChangedAt());
    }
}
