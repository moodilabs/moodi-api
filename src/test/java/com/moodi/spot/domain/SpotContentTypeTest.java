package com.moodi.spot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpotContentTypeTest {

    @ParameterizedTest
    @CsvSource({
            "관광지, TOURIST_ATTRACTION",
            "문화시설, CULTURAL_FACILITY",
            "축제공연행사, FESTIVAL",
            "여행코스, TRAVEL_COURSE",
            "레포츠, LEISURE_SPORTS",
            "숙박, ACCOMMODATION",
            "쇼핑, SHOPPING",
            "음식점, RESTAURANT"
    })
    @DisplayName("한글 라벨로 SpotContentType을 변환한다")
    void from_label_converts_korean_label(String label, SpotContentType expected) {
        assertThat(SpotContentType.fromLabel(label)).isEqualTo(expected);
    }

    @Test
    @DisplayName("알 수 없는 라벨이면 예외를 던진다")
    void from_label_throws_for_unknown_label() {
        assertThatThrownBy(() -> SpotContentType.fromLabel("존재하지않음"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("숙박과 음식점만 routeExcluded가 true이다")
    void route_excluded_only_for_accommodation_and_restaurant() {
        assertThat(SpotContentType.ACCOMMODATION.isRouteExcluded()).isTrue();
        assertThat(SpotContentType.RESTAURANT.isRouteExcluded()).isTrue();
        assertThat(SpotContentType.TOURIST_ATTRACTION.isRouteExcluded()).isFalse();
        assertThat(SpotContentType.SHOPPING.isRouteExcluded()).isFalse();
    }
}
