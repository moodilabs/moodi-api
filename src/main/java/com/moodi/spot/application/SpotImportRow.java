package com.moodi.spot.application;

public record SpotImportRow(
        String contentId,
        String contentType,
        String area,
        String district,
        String neighborhood,
        String source,
        Double longitude,
        Double latitude,
        String tel,
        boolean routeExcluded,
        String lclsSystm1,
        String lclsSystm2,
        String lclsSystm3,
        String homepage,
        String title,
        String overview,
        String addr1,
        String addr2,
        String imageUrl
) {
}
