package com.moodi.spot.support;

import com.moodi.spot.domain.SpotImage;

public class SpotImageFixture {

    private static final String DEFAULT_IMAGE_URL = "https://tong.visitkorea.or.kr/cms/resource/09/3303909_image2_1.jpg";

    public static SpotImage createPrimary(Long spotId) {
        return SpotImage.createPrimary(spotId, DEFAULT_IMAGE_URL);
    }

    public static SpotImage createPrimary(Long spotId, String imageUrl) {
        return SpotImage.createPrimary(spotId, imageUrl);
    }

    private SpotImageFixture() {
    }
}
