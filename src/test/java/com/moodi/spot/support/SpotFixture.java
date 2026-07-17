package com.moodi.spot.support;

import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import org.springframework.test.util.ReflectionTestUtils;

public class SpotFixture {

    private static final String DEFAULT_CONTENT_ID = "2733967";
    private static final SpotContentType DEFAULT_CONTENT_TYPE = SpotContentType.TOURIST_ATTRACTION;
    private static final String DEFAULT_AREA = "서울";
    private static final String DEFAULT_SOURCE = "kor_service";
    private static final Double DEFAULT_LONGITUDE = 126.9846467509;
    private static final Double DEFAULT_LATITUDE = 37.5820334711;

    public static Spot create() {
        return Spot.create(DEFAULT_CONTENT_ID, DEFAULT_CONTENT_TYPE, DEFAULT_AREA, DEFAULT_SOURCE,
                DEFAULT_LONGITUDE, DEFAULT_LATITUDE, null, "HS", "HS03", "HS030200");
    }

    public static Spot create(SpotContentType contentType) {
        return Spot.create(DEFAULT_CONTENT_ID, contentType, DEFAULT_AREA, DEFAULT_SOURCE,
                DEFAULT_LONGITUDE, DEFAULT_LATITUDE, null, null, null, null);
    }

    public static Spot create(String contentId, String source) {
        return Spot.create(contentId, DEFAULT_CONTENT_TYPE, DEFAULT_AREA, source,
                DEFAULT_LONGITUDE, DEFAULT_LATITUDE, null, null, null, null);
    }

    public static Spot createWithId(Long id) {
        Spot spot = create();
        ReflectionTestUtils.setField(spot, "id", id);
        return spot;
    }

    public static Spot createWithId(Long id, String contentId, String source) {
        Spot spot = create(contentId, source);
        ReflectionTestUtils.setField(spot, "id", id);
        return spot;
    }

    private SpotFixture() {
    }
}
