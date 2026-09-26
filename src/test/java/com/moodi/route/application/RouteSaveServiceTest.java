package com.moodi.route.application;

import com.moodi.route.application.RouteSaveCommand.DayCommand;
import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteDay;
import com.moodi.route.domain.RouteRepository;
import com.moodi.route.domain.RouteSpotType;
import com.moodi.route.domain.TravelMode;
import com.moodi.route.support.RouteFixture;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RouteSaveServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private SpotSnapshotReader spotSnapshotReader;

    @Mock
    private LegCalculator legCalculator;

    @InjectMocks
    private RouteSaveService routeSaveService;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 8, 10);
    private static final LocalDate END = LocalDate.of(2026, 8, 11);

    @Test
    @DisplayName("신규 루트 저장 성공")
    void save_route_success() {
        // given
        RouteSaveCommand command = new RouteSaveCommand(
                MEMBER_ID, "서울 감성 여행", START, END,
                List.of(
                        new DayCommand(1, START, List.of(1L, 2L)),
                        new DayCommand(2, END, List.of(3L))
                )
        );

        given(spotSnapshotReader.readBySpotIds(anyList()))
                .willReturn(List.of(
                        createSnapshot(1L, 37.55, 127.05),
                        createSnapshot(2L, 37.56, 127.06),
                        createSnapshot(3L, 37.57, 127.07)
                ));
        given(legCalculator.calculate(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(Optional.of(new LegResult(TravelMode.WALK, 600, 800, null)));
        given(routeRepository.save(any(Route.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        RouteSaveResult result = routeSaveService.save(command);

        // then
        assertThat(result.title()).isEqualTo("서울 감성 여행");
        assertThat(result.days()).hasSize(2);
        assertThat(result.days().get(0).spots()).hasSize(2);
        assertThat(result.days().get(0).legs()).hasSize(1);
        assertThat(result.days().get(1).spots()).hasSize(1);
        assertThat(result.days().get(1).legs()).isEmpty();
        assertThat(result.publicId()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 스팟 포함 시 실패")
    void save_route_spot_not_found() {
        // given
        RouteSaveCommand command = new RouteSaveCommand(
                MEMBER_ID, "여행", START, START,
                List.of(new DayCommand(1, START, List.of(1L, 999L)))
        );

        given(spotSnapshotReader.readBySpotIds(anyList()))
                .willReturn(List.of(createSnapshot(1L, 37.55, 127.05)));

        // when & then
        assertThatThrownBy(() -> routeSaveService.save(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPOT_NOT_FOUND);
    }

    @Test
    @DisplayName("루트 수정 — 변경된 Day만 Leg 재계산")
    void update_route_changed_day_only() {
        // given
        UUID publicId = UUID.randomUUID();
        Route existingRoute = RouteFixture.createRoute(
                MEMBER_ID, "기존 제목", START, END,
                List.of(
                        RouteFixture.createDay(1, START, 2),  // spotId 1, 2
                        RouteFixture.createDay(2, END, 2)     // spotId 1, 2
                )
        );

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(existingRoute));

        // Day 1은 동일(spotId 1,2), Day 2는 변경(spotId 3,4)
        List<Long> existingDay1SpotIds = existingRoute.getDays().get(0).getSpots().stream()
                .map(s -> s.getSpotId())
                .toList();

        RouteSaveCommand command = new RouteSaveCommand(
                MEMBER_ID, "수정된 제목", START, END,
                List.of(
                        new DayCommand(1, START, existingDay1SpotIds),
                        new DayCommand(2, END, List.of(3L, 4L))
                )
        );

        given(spotSnapshotReader.readBySpotIds(List.of(3L, 4L)))
                .willReturn(List.of(
                        createSnapshot(3L, 37.57, 127.07),
                        createSnapshot(4L, 37.58, 127.08)
                ));
        given(legCalculator.calculate(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(Optional.of(new LegResult(TravelMode.PUBLIC_TRANSIT, 1200, 5000, "https://map.kakao.com")));

        // when
        RouteSaveResult result = routeSaveService.update(publicId, command);

        // then
        assertThat(result.title()).isEqualTo("수정된 제목");
        assertThat(result.days()).hasSize(2);

        // Day 1은 기존 그대로
        assertThat(result.days().get(0).spots().get(0).spotId())
                .isEqualTo(existingDay1SpotIds.get(0));

        // Day 2는 새로 조립
        assertThat(result.days().get(1).spots().get(0).spotId()).isEqualTo(3L);
        assertThat(result.days().get(1).spots().get(1).spotId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("루트 수정 — 제목만 변경 시 스냅샷 조회·Leg 계산 없음")
    void update_route_title_only() {
        // given
        UUID publicId = UUID.randomUUID();
        Route existingRoute = RouteFixture.createRoute(
                MEMBER_ID, "기존 제목", START, END,
                List.of(
                        RouteFixture.createDay(1, START, 2),
                        RouteFixture.createDay(2, END, 1)
                )
        );

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(existingRoute));

        List<Long> day1SpotIds = existingRoute.getDays().get(0).getSpots().stream()
                .map(s -> s.getSpotId()).toList();
        List<Long> day2SpotIds = existingRoute.getDays().get(1).getSpots().stream()
                .map(s -> s.getSpotId()).toList();

        RouteSaveCommand command = new RouteSaveCommand(
                MEMBER_ID, "새 제목", START, END,
                List.of(
                        new DayCommand(1, START, day1SpotIds),
                        new DayCommand(2, END, day2SpotIds)
                )
        );

        // when
        RouteSaveResult result = routeSaveService.update(publicId, command);

        // then
        assertThat(result.title()).isEqualTo("새 제목");
        verify(spotSnapshotReader, never()).readBySpotIds(anyList());
        verify(legCalculator, never()).calculate(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("루트 수정 — 바뀐 Day는 기존 엔티티를 재사용하고 flush 뒤 다시 채운다")
    void update_route_reuses_changed_day_entity() {
        // given
        UUID publicId = UUID.randomUUID();
        Route existingRoute = RouteFixture.createRoute(
                MEMBER_ID, "기존 제목", START, END,
                List.of(
                        RouteFixture.createDay(1, START, 2),
                        RouteFixture.createDay(2, END, 1)
                )
        );
        RouteDay day1Before = existingRoute.getDays().get(0);

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(existingRoute));
        given(spotSnapshotReader.readBySpotIds(List.of(7L)))
                .willReturn(List.of(createSnapshot(7L, 37.59, 127.09)));

        List<Long> day2SpotIds = existingRoute.getDays().get(1).getSpots().stream()
                .map(s -> s.getSpotId()).toList();

        RouteSaveCommand command = new RouteSaveCommand(
                MEMBER_ID, "기존 제목", START, END,
                List.of(
                        new DayCommand(1, START, List.of(7L)),
                        new DayCommand(2, END, day2SpotIds)
                )
        );

        // when
        RouteSaveResult result = routeSaveService.update(publicId, command);

        // then: 같은 day_number 를 새 Day 로 갈아끼우면 uk_route_day_route_day_number 에 걸린다 —
        // 엔티티는 그대로 두고 안의 일정만 바뀌어야 한다.
        // RouteSaveResult 변환 후에는 엔티티 동일성(isSameAs) 대신 원본 엔티티로 검증한다.
        assertThat(existingRoute.getDays().get(0)).isSameAs(day1Before);
        assertThat(result.days().get(0).spots()).hasSize(1);
        assertThat(result.days().get(0).spots().get(0).spotId()).isEqualTo(7L);
        assertThat(result.days().get(0).spots().get(0).sequence()).isEqualTo(1);
        // 비운 스팟의 DELETE 가 새 INSERT 보다 먼저 나가야 uk_route_spot_day_sequence 를 피한다.
        verify(routeRepository).flush();
    }

    @Test
    @DisplayName("루트 수정 — 존재하지 않는 루트")
    void update_route_not_found() {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeRepository.findByPublicId(publicId)).willReturn(Optional.empty());

        RouteSaveCommand command = new RouteSaveCommand(
                MEMBER_ID, "제목", START, START,
                List.of(new DayCommand(1, START, List.of(1L)))
        );

        // when & then
        assertThatThrownBy(() -> routeSaveService.update(publicId, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }

    @Test
    @DisplayName("루트 수정 — 소유자가 아니면 실패")
    void update_route_forbidden() {
        // given
        UUID publicId = UUID.randomUUID();
        Route existingRoute = RouteFixture.createRoute(
                MEMBER_ID, "제목", START, START,
                List.of(RouteFixture.createDay(1, START, 1))
        );

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(existingRoute));

        UUID otherMemberId = UUID.randomUUID();
        RouteSaveCommand command = new RouteSaveCommand(
                otherMemberId, "제목", START, START,
                List.of(new DayCommand(1, START, List.of(1L)))
        );

        // when & then
        assertThatThrownBy(() -> routeSaveService.update(publicId, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_FORBIDDEN);
    }

    @Test
    @DisplayName("마지막 Day에 스팟 추가 성공")
    void add_spot_to_last_day_success() {
        // given
        UUID publicId = UUID.randomUUID();
        Route existingRoute = RouteFixture.createRoute(
                MEMBER_ID, "서울 여행", START, END,
                List.of(
                        RouteFixture.createDay(1, START, 2),
                        RouteFixture.createDay(2, END, 1)
                )
        );

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(existingRoute));

        Long existingSpotId = existingRoute.getDays().get(1).getSpots().get(0).getSpotId();
        Long newSpotId = 99L;

        given(spotSnapshotReader.readBySpotIds(List.of(newSpotId)))
                .willReturn(List.of(
                        createSnapshot(newSpotId, 37.58, 127.08)
                ));
        given(legCalculator.calculate(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(Optional.of(new LegResult(TravelMode.WALK, 900, 1200, null)));

        RouteDay lastDayBefore = existingRoute.getDays().get(1);

        // when
        RouteSaveResult result = routeSaveService.addSpotToLastDay(publicId, MEMBER_ID, newSpotId);

        // then
        assertThat(result.days().get(1).spots()).hasSize(2);
        assertThat(result.days().get(1).spots().get(1).spotId()).isEqualTo(newSpotId);
        assertThat(result.days().get(1).spots().get(1).sequence()).isEqualTo(2);
        assertThat(result.days().get(1).legs()).hasSize(1);
        assertThat(result.days().get(1).legs().get(0).fromSequence()).isEqualTo(1);
        assertThat(result.days().get(1).legs().get(0).toSequence()).isEqualTo(2);
        // 영속 상태의 Day를 새 인스턴스로 갈아끼우지 않고 그대로 덧붙인다 (day_number 유니크 제약 회피)
        assertThat(existingRoute.getDays().get(1)).isSameAs(lastDayBefore);
        // Day 1은 변경 없음
        assertThat(result.days().get(0).spots()).hasSize(2);
    }

    @Test
    @DisplayName("마지막 Day에 스팟 추가 — 좌표가 없는 스팟은 이동정보를 계산하지 않는다")
    void add_spot_to_last_day_without_coordinates() {
        // given
        UUID publicId = UUID.randomUUID();
        Route existingRoute = RouteFixture.createRoute(
                MEMBER_ID, "서울 여행", START, START,
                List.of(RouteFixture.createDay(1, START, 1))
        );

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(existingRoute));

        Long newSpotId = 99L;
        given(spotSnapshotReader.readBySpotIds(List.of(newSpotId)))
                .willReturn(List.of(new SpotSnapshot(
                        newSpotId, "좌표 없는 스팟", null, "서울", "성동구",
                        null, null, RouteSpotType.TOURIST_ATTRACTION, null,
                        List.of()
                )));

        // when
        RouteSaveResult result = routeSaveService.addSpotToLastDay(publicId, MEMBER_ID, newSpotId);

        // then
        RouteSaveResult.DayResult lastDay = result.days().get(0);
        assertThat(lastDay.spots()).hasSize(2);
        assertThat(lastDay.legs()).hasSize(1);
        assertThat(lastDay.legs().get(0).travelMode()).isEqualTo(TravelMode.UNAVAILABLE);
        verify(legCalculator, never()).calculate(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("마지막 Day가 비어 있으면 스팟만 추가하고 이동정보는 만들지 않는다")
    void add_spot_to_empty_last_day() {
        // given
        UUID publicId = UUID.randomUUID();
        Route existingRoute = RouteFixture.createRoute(
                MEMBER_ID, "서울 여행", START, START,
                List.of(RouteFixture.createDay(1, START, 0))
        );

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(existingRoute));

        Long newSpotId = 99L;
        given(spotSnapshotReader.readBySpotIds(List.of(newSpotId)))
                .willReturn(List.of(createSnapshot(newSpotId, 37.58, 127.08)));

        // when
        RouteSaveResult result = routeSaveService.addSpotToLastDay(publicId, MEMBER_ID, newSpotId);

        // then
        RouteSaveResult.DayResult lastDay = result.days().get(0);
        assertThat(lastDay.spots()).hasSize(1);
        assertThat(lastDay.spots().get(0).sequence()).isEqualTo(1);
        assertThat(lastDay.legs()).isEmpty();
        verify(legCalculator, never()).calculate(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("마지막 Day에 스팟 추가 — 존재하지 않는 루트")
    void add_spot_to_last_day_route_not_found() {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeRepository.findByPublicId(publicId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> routeSaveService.addSpotToLastDay(publicId, MEMBER_ID, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }

    @Test
    @DisplayName("마지막 Day에 스팟 추가 — 소유자가 아니면 실패")
    void add_spot_to_last_day_forbidden() {
        // given
        UUID publicId = UUID.randomUUID();
        Route existingRoute = RouteFixture.createRoute(
                MEMBER_ID, "여행", START, START,
                List.of(RouteFixture.createDay(1, START, 1))
        );

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(existingRoute));

        UUID otherMemberId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> routeSaveService.addSpotToLastDay(publicId, otherMemberId, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_FORBIDDEN);
    }

    private SpotSnapshot createSnapshot(Long spotId, double lat, double lng) {
        return new SpotSnapshot(
                spotId, "스팟 " + spotId, "https://img.example.com/" + spotId + ".jpg",
                "서울", "성동구", lat, lng,
                RouteSpotType.TOURIST_ATTRACTION, null,
                List.of()
        );
    }
}
