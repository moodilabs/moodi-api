package com.moodi.spot.presentation.dto.admin;

import com.moodi.shared.mood.MoodTag;
import com.moodi.spot.application.dto.SpotAdminDetail;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminSpotDetailResponse(
        Long id, String contentId, String source, SpotContentType contentType, String area, String district,
        String neighborhood, Double latitude, Double longitude, String tel, String homepage, SpotStatus status,
        String statusReason, LocalDateTime statusChangedAt, boolean routeExcluded,
        TranslationResponse translation, String description, MoodResponse mood, List<ImageResponse> images,
        long bookmarkCount, LocalDateTime createdAt, LocalDateTime updatedAt
) {

    public record TranslationResponse(String title, String overview, String addr1, String addr2) {}

    public record MoodResponse(List<MoodTag> tags, Double confidence) {}

    public record ImageResponse(String url, boolean primary, int sortOrder) {}

    public static AdminSpotDetailResponse from(SpotAdminDetail d) {
        return new AdminSpotDetailResponse(d.id(), d.contentId(), d.source(), d.contentType(), d.area(),
                d.district(), d.neighborhood(), d.latitude(), d.longitude(), d.tel(), d.homepage(), d.status(),
                d.statusReason(), d.statusChangedAt(), d.routeExcluded(),
                d.translation() == null ? null : new TranslationResponse(d.translation().title(),
                        d.translation().overview(), d.translation().addr1(), d.translation().addr2()),
                d.description(),
                d.mood() == null ? null : new MoodResponse(d.mood().tags(), d.mood().confidence()),
                d.images().stream().map(i -> new ImageResponse(i.url(), i.primary(), i.sortOrder())).toList(),
                d.bookmarkCount(), d.createdAt(), d.updatedAt());
    }
}
