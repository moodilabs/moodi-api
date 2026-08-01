package com.moodi.route.domain;

import com.moodi.route.domain.RouteSpotType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StayDurationPolicyTest {

    @Test
    @DisplayName("관광지·레포츠는 120분")
    void tourist_attraction_and_leisure_120_minutes() {
        assertThat(StayDurationPolicy.getEstimatedMinutes(RouteSpotType.TOURIST_ATTRACTION)).isEqualTo(120);
        assertThat(StayDurationPolicy.getEstimatedMinutes(RouteSpotType.LEISURE_SPORTS)).isEqualTo(120);
    }

    @Test
    @DisplayName("문화시설·축제는 90분")
    void cultural_facility_and_festival_90_minutes() {
        assertThat(StayDurationPolicy.getEstimatedMinutes(RouteSpotType.CULTURAL_FACILITY)).isEqualTo(90);
        assertThat(StayDurationPolicy.getEstimatedMinutes(RouteSpotType.FESTIVAL)).isEqualTo(90);
    }

    @Test
    @DisplayName("쇼핑·여행코스는 60분")
    void shopping_and_travel_course_60_minutes() {
        assertThat(StayDurationPolicy.getEstimatedMinutes(RouteSpotType.SHOPPING)).isEqualTo(60);
        assertThat(StayDurationPolicy.getEstimatedMinutes(RouteSpotType.TRAVEL_COURSE)).isEqualTo(60);
    }
}
