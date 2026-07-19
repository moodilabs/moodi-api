package com.moodi.spot.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spot extends BaseEntity {

    private Long id;
    private String contentId;
    private SpotContentType contentType;
    private String area;
    private String source;
    private Double longitude;
    private Double latitude;
    private String tel;
    private boolean routeExcluded;
    private SpotStatus status;
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;
    private String homepage;

    private Spot(String contentId, SpotContentType contentType, String area, String source,
                 Double longitude, Double latitude, String tel,
                 String lclsSystm1, String lclsSystm2, String lclsSystm3, String homepage) {
        this.contentId = contentId;
        this.contentType = contentType;
        this.area = area;
        this.source = source;
        this.longitude = longitude;
        this.latitude = latitude;
        this.tel = tel;
        this.routeExcluded = contentType.isRouteExcluded();
        this.status = SpotStatus.TAGGING_PENDING;
        this.lclsSystm1 = lclsSystm1;
        this.lclsSystm2 = lclsSystm2;
        this.lclsSystm3 = lclsSystm3;
        this.homepage = homepage;
    }

    public static Spot create(String contentId, SpotContentType contentType, String area, String source,
                              Double longitude, Double latitude, String tel,
                              String lclsSystm1, String lclsSystm2, String lclsSystm3, String homepage) {
        return new Spot(contentId, contentType, area, source, longitude, latitude, tel,
                lclsSystm1, lclsSystm2, lclsSystm3, homepage);
    }
}
