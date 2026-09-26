package com.moodi.discovery.presentation.dto;

import com.moodi.discovery.application.RecommendedAreaService;
import com.moodi.discovery.domain.PickAreaLevel;

public record RecommendedAreaRequest(
        PickAreaLevel level,
        String region,
        String district,
        String neighborhood,
        String label,
        int sortOrder
) {
    public RecommendedAreaService.Command toCommand() {
        return new RecommendedAreaService.Command(
                level, region, district, neighborhood, label, sortOrder
        );
    }
}
