package com.moodi.route.application;

import com.moodi.route.domain.TravelMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LegCalculatorTest {

    @Mock
    private LegClient legClient;

    @InjectMocks
    private LegCalculator legCalculator;

    // 서울 성수동 두 지점 (약 500m)
    private static final double START_LNG = 127.0560;
    private static final double START_LAT = 37.5445;
    private static final double NEAR_LNG = 127.0590;
    private static final double NEAR_LAT = 37.5475;

    // 서울 → 부산 (약 325km)
    private static final double FAR_LNG = 129.0756;
    private static final double FAR_LAT = 35.1796;

    @Test
    @DisplayName("도보가 대중교통보다 빠르면 도보 선택")
    void walk_faster_than_transit_returns_walk() {
        // given
        LegResult walkResult = new LegResult(TravelMode.WALK, 420, 500, null);
        LegResult transitResult = new LegResult(TravelMode.PUBLIC_TRANSIT, 600, 800, null);
        given(legClient.findWalkRoute(START_LNG, START_LAT, NEAR_LNG, NEAR_LAT))
                .willReturn(Optional.of(walkResult));
        given(legClient.findTransitRoute(START_LNG, START_LAT, NEAR_LNG, NEAR_LAT))
                .willReturn(Optional.of(transitResult));

        // when
        Optional<LegResult> result = legCalculator.calculate(START_LNG, START_LAT, NEAR_LNG, NEAR_LAT);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().travelMode()).isEqualTo(TravelMode.WALK);
    }

    @Test
    @DisplayName("대중교통이 도보보다 빠르면 대중교통 선택")
    void transit_faster_than_walk_returns_transit() {
        // given
        LegResult walkResult = new LegResult(TravelMode.WALK, 50000, 325000, null);
        LegResult transitResult = new LegResult(TravelMode.PUBLIC_TRANSIT, 10800, 325000, null);
        given(legClient.findWalkRoute(START_LNG, START_LAT, FAR_LNG, FAR_LAT))
                .willReturn(Optional.of(walkResult));
        given(legClient.findTransitRoute(START_LNG, START_LAT, FAR_LNG, FAR_LAT))
                .willReturn(Optional.of(transitResult));

        // when
        Optional<LegResult> result = legCalculator.calculate(START_LNG, START_LAT, FAR_LNG, FAR_LAT);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().travelMode()).isEqualTo(TravelMode.PUBLIC_TRANSIT);
    }

    @Test
    @DisplayName("소요 시간이 동일하면 도보 우선")
    void same_duration_returns_walk() {
        // given
        LegResult walkResult = new LegResult(TravelMode.WALK, 600, 500, null);
        LegResult transitResult = new LegResult(TravelMode.PUBLIC_TRANSIT, 600, 800, null);
        given(legClient.findWalkRoute(START_LNG, START_LAT, NEAR_LNG, NEAR_LAT))
                .willReturn(Optional.of(walkResult));
        given(legClient.findTransitRoute(START_LNG, START_LAT, NEAR_LNG, NEAR_LAT))
                .willReturn(Optional.of(transitResult));

        // when
        Optional<LegResult> result = legCalculator.calculate(START_LNG, START_LAT, NEAR_LNG, NEAR_LAT);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().travelMode()).isEqualTo(TravelMode.WALK);
    }

    @Test
    @DisplayName("도보 조회 실패 시 대중교통 fallback")
    void walk_unavailable_fallback_to_transit() {
        // given
        LegResult transitResult = new LegResult(TravelMode.PUBLIC_TRANSIT, 10800, 325000, null);
        given(legClient.findWalkRoute(START_LNG, START_LAT, FAR_LNG, FAR_LAT))
                .willReturn(Optional.empty());
        given(legClient.findTransitRoute(START_LNG, START_LAT, FAR_LNG, FAR_LAT))
                .willReturn(Optional.of(transitResult));

        // when
        Optional<LegResult> result = legCalculator.calculate(START_LNG, START_LAT, FAR_LNG, FAR_LAT);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().travelMode()).isEqualTo(TravelMode.PUBLIC_TRANSIT);
    }

    @Test
    @DisplayName("대중교통 조회 실패 시 도보 fallback")
    void transit_unavailable_fallback_to_walk() {
        // given
        LegResult walkResult = new LegResult(TravelMode.WALK, 50000, 325000, null);
        given(legClient.findWalkRoute(START_LNG, START_LAT, FAR_LNG, FAR_LAT))
                .willReturn(Optional.of(walkResult));
        given(legClient.findTransitRoute(START_LNG, START_LAT, FAR_LNG, FAR_LAT))
                .willReturn(Optional.empty());

        // when
        Optional<LegResult> result = legCalculator.calculate(START_LNG, START_LAT, FAR_LNG, FAR_LAT);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().travelMode()).isEqualTo(TravelMode.WALK);
    }

    @Test
    @DisplayName("도보·대중교통 모두 실패하면 빈 결과")
    void both_unavailable_returns_empty() {
        // given
        given(legClient.findWalkRoute(START_LNG, START_LAT, FAR_LNG, FAR_LAT))
                .willReturn(Optional.empty());
        given(legClient.findTransitRoute(START_LNG, START_LAT, FAR_LNG, FAR_LAT))
                .willReturn(Optional.empty());

        // when
        Optional<LegResult> result = legCalculator.calculate(START_LNG, START_LAT, FAR_LNG, FAR_LAT);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("동일 위치면 API 호출 없이 도보 0초 반환")
    void same_location_returns_walk_zero_without_api_call() {
        // given
        double lng = 127.0560;
        double lat = 37.5445;

        // when
        Optional<LegResult> result = legCalculator.calculate(lng, lat, lng, lat);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().travelMode()).isEqualTo(TravelMode.WALK);
        assertThat(result.get().durationSeconds()).isZero();
        assertThat(result.get().distanceMeters()).isZero();
        verify(legClient, never()).findWalkRoute(lng, lat, lng, lat);
        verify(legClient, never()).findTransitRoute(lng, lat, lng, lat);
    }

    @Test
    @DisplayName("Haversine 직선거리 계산 정확성")
    void haversine_distance_calculation() {
        // 서울시청 → 부산시청 약 325km
        double distance = LegCalculator.calculateStraightDistance(
                37.5666, 126.9784, 35.1796, 129.0756);

        assertThat(distance).isBetween(300_000.0, 350_000.0);
    }
}
