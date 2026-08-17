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

    public SpotImportRow withImageUrl(String newImageUrl) {
        return new SpotImportRow(
                contentId, contentType, area, district, neighborhood, source,
                longitude, latitude, tel, routeExcluded,
                lclsSystm1, lclsSystm2, lclsSystm3, homepage,
                title, overview, addr1, addr2, newImageUrl
        );
    }
}
