package com.moodi.spot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpotTest {

    @Test
    @DisplayName("Spot 생성 시 상태는 TAGGING_PENDING이다")
    void create_sets_tagging_pending_status() {
        Spot spot = Spot.create("123", SpotContentType.TOURIST_ATTRACTION, "서울", "kor_service",
                126.98, 37.58, null, "HS", "HS03", null, null);

        assertThat(spot.getStatus()).isEqualTo(SpotStatus.TAGGING_PENDING);
    }

    @Test
    @DisplayName("숙박 유형은 routeExcluded가 true이다")
    void create_accommodation_sets_route_excluded_true() {
        Spot spot = Spot.create("123", SpotContentType.ACCOMMODATION, "부산", "kor_service",
                129.05, 35.15, null, null, null, null, null);

        assertThat(spot.isRouteExcluded()).isTrue();
    }

    @Test
    @DisplayName("음식점 유형은 routeExcluded가 true이다")
    void create_restaurant_sets_route_excluded_true() {
        Spot spot = Spot.create("456", SpotContentType.RESTAURANT, "서울", "kor_service",
                126.98, 37.58, "02-1234-5678", null, null, null, null);

        assertThat(spot.isRouteExcluded()).isTrue();
    }

    @Test
    @DisplayName("관광지 유형은 routeExcluded가 false이다")
    void create_tourist_attraction_sets_route_excluded_false() {
        Spot spot = Spot.create("789", SpotContentType.TOURIST_ATTRACTION, "서울", "kor_service",
                126.98, 37.58, null, null, null, null, null);

        assertThat(spot.isRouteExcluded()).isFalse();
    }

    @Test
    @DisplayName("Spot 생성 시 모든 필드가 정상 세팅된다")
    void create_sets_all_fields() {
        Spot spot = Spot.create("2733967", SpotContentType.CULTURAL_FACILITY, "서울", "kor_service",
                126.98, 37.58, "02-123-4567", "HS", "HS03", "HS030200", "https://example.com");

        assertThat(spot.getContentId()).isEqualTo("2733967");
        assertThat(spot.getContentType()).isEqualTo(SpotContentType.CULTURAL_FACILITY);
        assertThat(spot.getArea()).isEqualTo("서울");
        assertThat(spot.getSource()).isEqualTo("kor_service");
        assertThat(spot.getLongitude()).isEqualTo(126.98);
        assertThat(spot.getLatitude()).isEqualTo(37.58);
        assertThat(spot.getTel()).isEqualTo("02-123-4567");
        assertThat(spot.getLclsSystm1()).isEqualTo("HS");
    }
}
