package com.moodi.route.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteSpot {

    private Long id;
    private Long spotId;
    private int sequence;
    private int estimatedMinutes;

    // Spot 스냅샷
    private String spotTitle;
    private String spotImageUrl;
    private String spotArea;
    private String spotDistrict;
    private Double spotLatitude;
    private Double spotLongitude;
    private String spotContentType;
    private String spotDescription;

    private RouteSpot(Long spotId, int sequence, int estimatedMinutes,
                      String spotTitle, String spotImageUrl, String spotArea, String spotDistrict,
                      Double spotLatitude, Double spotLongitude, String spotContentType,
                      String spotDescription) {
        this.spotId = spotId;
        this.sequence = sequence;
        this.estimatedMinutes = estimatedMinutes;
        this.spotTitle = spotTitle;
        this.spotImageUrl = spotImageUrl;
        this.spotArea = spotArea;
        this.spotDistrict = spotDistrict;
        this.spotLatitude = spotLatitude;
        this.spotLongitude = spotLongitude;
        this.spotContentType = spotContentType;
        this.spotDescription = spotDescription;
    }

    public static RouteSpot create(Long spotId, int sequence, int estimatedMinutes,
                                    String spotTitle, String spotImageUrl,
                                    String spotArea, String spotDistrict,
                                    Double spotLatitude, Double spotLongitude,
                                    String spotContentType, String spotDescription) {
        return new RouteSpot(spotId, sequence, estimatedMinutes,
                spotTitle, spotImageUrl, spotArea, spotDistrict,
                spotLatitude, spotLongitude, spotContentType, spotDescription);
    }
}
