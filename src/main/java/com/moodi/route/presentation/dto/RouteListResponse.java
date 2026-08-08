package com.moodi.route.presentation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RouteListResponse(
        UUID publicId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        int totalDays,
        int spotCount,
        LocalDateTime updatedAt
) {
}
