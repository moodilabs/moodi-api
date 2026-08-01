package com.moodi.route.application;

import com.moodi.route.domain.TravelMode;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.spot.domain.SpotContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RouteGenerateServiceTest {

    @Mock
    private SpotSnapshotReader spotSnapshotReader;

    @Spy
    private SpotDistributor spotDistributor;

    @Mock
    private LegCalculator legCalculator;

    @Spy
    private DayScheduleValidator dayScheduleValidator;

    @Mock
    private RouteTitleGenerator titleGenerator;

    @InjectMocks
    private RouteGenerateService routeGenerateService;

    private static final LocalDate FUTURE_START = LocalDate.now().plusDays(7);
    private static final LocalDate FUTURE_END = LocalDate.now().plusDays(8);

    @Test
    @DisplayName("정상 루트 생성 — 2일 3스팟")
    void generate_route_success() {
        // given
        RouteGenerateCommand command = new RouteGenerateCommand(
                List.of(1L, 2L, 3L), List.of("서울"), FUTURE_START, FUTURE_END);

        List<SpotSnapshot> snapshots = List.of(
                createSnapshot(1L, 37.55, 127.05),
                createSnapshot(2L, 37.56, 127.06),
                createSnapshot(3L, 37.57, 127.07)
        );
        given(spotSnapshotReader.readBySpotIds(command.spotIds())).willReturn(snapshots);
        given(legCalculator.calculate(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(Optional.of(new LegResult(TravelMode.WALK, 600, 800, null)));
        given(titleGenerator.generate(List.of("서울"), 2)).willReturn("서울 1박 2일 코스");

        // when
        RouteGenerateResult result = routeGenerateService.generate(command);

        // then
        assertThat(result.title()).isEqualTo("서울 1박 2일 코스");
        assertThat(result.days()).hasSize(2);
        assertThat(result.startDate()).isEqualTo(FUTURE_START);
        assertThat(result.endDate()).isEqualTo(FUTURE_END);

        int totalSpots = result.days().stream()
                .mapToInt(day -> day.spots().size())
                .sum();
        assertThat(totalSpots).isEqualTo(3);
    }

    @Test
    @DisplayName("과거 날짜 요청 시 실패")
    void generate_route_past_date() {
        RouteGenerateCommand command = new RouteGenerateCommand(
                List.of(1L), List.of(), LocalDate.now().minusDays(1), LocalDate.now());

        assertThatThrownBy(() -> routeGenerateService.generate(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_PAST_START_DATE);
    }

    @Test
    @DisplayName("6일 이상 기간 요청 시 실패")
    void generate_route_too_many_days() {
        RouteGenerateCommand command = new RouteGenerateCommand(
                List.of(1L), List.of(), FUTURE_START, FUTURE_START.plusDays(5));

        assertThatThrownBy(() -> routeGenerateService.generate(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("중복 스팟 ID 요청 시 실패")
    void generate_route_duplicate_spot_ids() {
        RouteGenerateCommand command = new RouteGenerateCommand(
                List.of(1L, 1L, 2L), List.of(), FUTURE_START, FUTURE_START);

        assertThatThrownBy(() -> routeGenerateService.generate(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("스팟 수 > 일수 × 6이면 실패")
    void generate_route_too_many_spots_for_days() {
        RouteGenerateCommand command = new RouteGenerateCommand(
                List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L), List.of(),
                FUTURE_START, FUTURE_START);

        assertThatThrownBy(() -> routeGenerateService.generate(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_TOO_MANY_SPOTS_FOR_DAYS);
    }

    @Test
    @DisplayName("존재하지 않는 스팟 포함 시 실패")
    void generate_route_spot_not_found() {
        RouteGenerateCommand command = new RouteGenerateCommand(
                List.of(1L, 999L), List.of(), FUTURE_START, FUTURE_START);
        given(spotSnapshotReader.readBySpotIds(command.spotIds()))
                .willReturn(List.of(createSnapshot(1L, 37.55, 127.05)));

        assertThatThrownBy(() -> routeGenerateService.generate(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPOT_NOT_FOUND);
    }

    private SpotSnapshot createSnapshot(Long spotId, double lat, double lng) {
        return new SpotSnapshot(
                spotId, "스팟 " + spotId, "https://img.example.com/" + spotId + ".jpg",
                "서울", "성동구", lat, lng,
                SpotContentType.TOURIST_ATTRACTION, null
        );
    }
}
