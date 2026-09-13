package com.moodi.spot.application.dto;

import com.moodi.shared.mood.MoodTag;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SpotAdminDetail(
        Long id,
        String contentId,
        String source,
        SpotContentType contentType,
        String area,
        String district,
        String neighborhood,
        Double latitude,
        Double longitude,
        String tel,
        String homepage,
        SpotStatus status,
        String statusReason,
        LocalDateTime statusChangedAt,
        boolean routeExcluded,
        Translation translation,
        String description,
        Mood mood,
        List<Image> images,
        long bookmarkCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record Translation(String title, String overview, String addr1, String addr2) {}

    public record Mood(List<MoodTag> tags, Double confidence) {}

    public record Image(String url, boolean primary, int sortOrder) {}
}
