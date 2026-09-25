package com.moodi.spot.presentation.dto.admin;

import java.util.Map;

public record MoodTaggingSummaryResponse(
        Map<String, Long> statusCounts,
        long total
) {}
