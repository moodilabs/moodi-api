package com.moodi.spot.application;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SpotCsvRow {

    private final String contentId;
    private final String title;
    private final String contentType;
    private final String area;
    private final String source;
    private final String overview;
    private final String spotImage;
    private final String longitude;
    private final String latitude;
    private final String addr1;
    private final String addr2;
    private final String tel;
    private final String lclsSystm1;
    private final String lclsSystm2;
    private final String lclsSystm3;
}
