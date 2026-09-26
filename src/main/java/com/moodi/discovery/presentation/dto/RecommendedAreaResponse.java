package com.moodi.discovery.presentation.dto;

import com.moodi.discovery.application.RecommendedAreaService;
import com.moodi.discovery.domain.PickAreaLevel;

public record RecommendedAreaResponse(
        Long id,
        PickAreaLevel level,
        String region,
        String district,
        String neighborhood,
        String label,
        int sortOrder
) {
    public static RecommendedAreaResponse from(RecommendedAreaService.View view) {
        return new RecommendedAreaResponse(
                view.id(), view.level(), view.region(),
                view.district(), view.neighborhood(),
                view.label(), view.sortOrder()
        );
    }
}
