package com.moodi.spot.support;

import com.moodi.spot.domain.SpotTranslation;

public class SpotTranslationFixture {

    private static final String DEFAULT_LOCALE = "ko-KR";
    private static final String DEFAULT_TITLE = "가회동성당";
    private static final String DEFAULT_OVERVIEW = "가회동성당은 종로구에 위치한 성당이다.";
    private static final String DEFAULT_ADDR1 = "서울특별시 종로구 북촌로 57";

    public static SpotTranslation create(Long spotId) {
        return SpotTranslation.create(spotId, DEFAULT_LOCALE, DEFAULT_TITLE,
                DEFAULT_OVERVIEW, DEFAULT_ADDR1, null);
    }

    public static SpotTranslation create(Long spotId, String locale, String title) {
        return SpotTranslation.create(spotId, locale, title, DEFAULT_OVERVIEW, DEFAULT_ADDR1, null);
    }

    private SpotTranslationFixture() {
    }
}
