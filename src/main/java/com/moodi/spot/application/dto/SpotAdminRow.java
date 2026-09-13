package com.moodi.spot.application.dto;

import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotStatus;

import java.time.LocalDateTime;

public record SpotAdminRow(Long id, String title, SpotContentType contentType, String area, String district,
                           SpotStatus status, boolean routeExcluded, long bookmarkCount, LocalDateTime createdAt,
                           LocalDateTime statusChangedAt) {
}
