package com.moodi.route.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RouteListRow(
        Long id,
        UUID publicId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime updatedAt
) {
}
