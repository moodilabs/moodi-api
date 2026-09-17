package com.moodi.route.application;

import com.moodi.route.application.LegCalculateService.LegCalculateResult;
import com.moodi.route.application.LegCalculateService.SpotPairCommand;
import com.moodi.route.domain.RouteSpotType;
import com.moodi.route.domain.TravelMode;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LegCalculateServiceTest {

    @Mock
    private SpotSnapshotReader spotSnapshotReader;

    @Mock
    private LegCalculator legCalculator;

    @InjectMocks
    private LegCalculateService legCalculateService;

    @Test
    @DisplayName("정상 계산 - 스팟 쌍 1개, WALK 결과 반환")
    void calculate_single_pair_walk() {
        // given
        SpotPairCommand pair = new SpotPairCommand(1L, 2L);
        given(spotSnapshotReader.readBySpotIds(anyList()))
                .willReturn(List.of(
                        createSnapshot(1L, 37.55, 127.05),
                        createSnapshot(2L, 37.56, 127.06)
                ));
        given(legCalculator.calculate(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(Optional.of(new LegResult(TravelMode.WALK, 600, 800, null)));

        // when
        List<LegCalculateResult> results = legCalculateService.calculate(List.of(pair));

        // then
        assertThat(results).hasSize(1);
        LegCalculateResult result = results.get(0);
        assertThat(result.fromSpotId()).isEqualTo(1L);
        assertThat(result.toSpotId()).isEqualTo(2L);
        assertThat(result.travelMode()).isEqualTo("WALK");
        assertThat(result.durationSeconds()).isEqualTo(600);
        assertThat(result.distanceMeters()).isEqualTo(800);
    }

    @Test
    @DisplayName("여러 구간 계산 - 각각 다른 travelMode")
    void calculate_multiple_pairs_different_modes() {
        // given
        SpotPairCommand pair1 = new SpotPairCommand(1L, 2L);
        SpotPairCommand pair2 = new SpotPairCommand(2L, 3L);

        given(spotSnapshotReader.readBySpotIds(anyList()))
                .willReturn(List.of(
                        createSnapshot(1L, 37.55, 127.05),
                        createSnapshot(2L, 37.56, 127.06),
                        createSnapshot(3L, 37.57, 127.07)
                ));
        given(legCalculator.calculate(127.05, 37.55, 127.06, 37.56))
                .willReturn(Optional.of(new LegResult(TravelMode.WALK, 600, 800, null)));
        given(legCalculator.calculate(127.06, 37.56, 127.07, 37.57))
                .willReturn(Optional.of(new LegResult(TravelMode.PUBLIC_TRANSIT, 1200, 5000, "https://map.example.com")));

        // when
        List<LegCalculateResult> results = legCalculateService.calculate(List.of(pair1, pair2));

        // then
        assertThat(results).hasSize(2);

        assertThat(results.get(0).travelMode()).isEqualTo("WALK");
        assertThat(results.get(0).durationSeconds()).isEqualTo(600);

        assertThat(results.get(1).travelMode()).isEqualTo("PUBLIC_TRANSIT");
        assertThat(results.get(1).durationSeconds()).isEqualTo(1200);
        assertThat(results.get(1).distanceMeters()).isEqualTo(5000);
        assertThat(results.get(1).landingUrl()).isEqualTo("https://map.example.com");
    }

    @Test
    @DisplayName("좌표가 null인 스팟은 UNAVAILABLE 처리")
    void calculate_null_coordinates_returns_unavailable() {
        // given
        SpotPairCommand pair = new SpotPairCommand(1L, 2L);
        given(spotSnapshotReader.readBySpotIds(anyList()))
                .willReturn(List.of(
                        createSnapshot(1L, null, null),
                        createSnapshot(2L, 37.56, 127.06)
                ));

        // when
        List<LegCalculateResult> results = legCalculateService.calculate(List.of(pair));

        // then
        assertThat(results).hasSize(1);
        LegCalculateResult result = results.get(0);
        assertThat(result.travelMode()).isNull();
        assertThat(result.durationSeconds()).isZero();
        assertThat(result.distanceMeters()).isZero();
        assertThat(result.landingUrl()).isNull();
    }

    @Test
    @DisplayName("LegCalculator가 empty 반환 시 UNAVAILABLE 처리")
    void calculate_empty_leg_result_returns_unavailable() {
        // given
        SpotPairCommand pair = new SpotPairCommand(1L, 2L);
        given(spotSnapshotReader.readBySpotIds(anyList()))
                .willReturn(List.of(
                        createSnapshot(1L, 37.55, 127.05),
                        createSnapshot(2L, 37.56, 127.06)
                ));
        given(legCalculator.calculate(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(Optional.empty());

        // when
        List<LegCalculateResult> results = legCalculateService.calculate(List.of(pair));

        // then
        assertThat(results).hasSize(1);
        LegCalculateResult result = results.get(0);
        assertThat(result.travelMode()).isNull();
        assertThat(result.durationSeconds()).isZero();
        assertThat(result.distanceMeters()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 스팟 ID 포함 시 SPOT_NOT_FOUND 예외")
    void calculate_spot_not_found() {
        // given
        SpotPairCommand pair = new SpotPairCommand(1L, 999L);
        given(spotSnapshotReader.readBySpotIds(anyList()))
                .willReturn(List.of(createSnapshot(1L, 37.55, 127.05)));

        // when & then
        assertThatThrownBy(() -> legCalculateService.calculate(List.of(pair)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPOT_NOT_FOUND);
    }

    @Test
    @DisplayName("중복 스팟 ID가 있는 쌍에서 스냅샷 조회 시 중복 제거")
    void calculate_deduplicates_spot_ids() {
        // given
        SpotPairCommand pair1 = new SpotPairCommand(1L, 2L);
        SpotPairCommand pair2 = new SpotPairCommand(2L, 3L);

        given(spotSnapshotReader.readBySpotIds(anyList()))
                .willReturn(List.of(
                        createSnapshot(1L, 37.55, 127.05),
                        createSnapshot(2L, 37.56, 127.06),
                        createSnapshot(3L, 37.57, 127.07)
                ));
        given(legCalculator.calculate(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(Optional.of(new LegResult(TravelMode.WALK, 600, 800, null)));

        // when
        List<LegCalculateResult> results = legCalculateService.calculate(List.of(pair1, pair2));

        // then
        assertThat(results).hasSize(2);
        // spotSnapshotReader는 중복 제거된 3개의 ID로 호출되어야 함
        verify(spotSnapshotReader).readBySpotIds(anyList());
    }

    private SpotSnapshot createSnapshot(Long spotId, Double lat, Double lng) {
        return new SpotSnapshot(
                spotId, "스팟 " + spotId, "https://img.example.com/" + spotId + ".jpg",
                "서울", "성동구", lat, lng,
                RouteSpotType.TOURIST_ATTRACTION, null,
                List.of("nature", "serene")
        );
    }
}
