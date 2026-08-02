package com.moodi.route.application;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteRepository;
import com.moodi.route.support.RouteFixture;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RouteQueryServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RouteQueryRepository routeQueryRepository;

    @InjectMocks
    private RouteQueryService routeQueryService;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 8, 10);
    private static final LocalDate END = LocalDate.of(2026, 8, 11);

    @Test
    @DisplayName("상세 조회 성공")
    void get_detail_success() {
        // given
        UUID publicId = UUID.randomUUID();
        Route route = RouteFixture.createRoute(
                MEMBER_ID, "Seoul vibes trip", START, END,
                List.of(RouteFixture.createDay(1, START, 2))
        );

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(route));

        // when
        Route result = routeQueryService.getDetail(publicId, MEMBER_ID);

        // then
        assertThat(result.getTitle()).isEqualTo("Seoul vibes trip");
        assertThat(result.getDays()).hasSize(1);
    }

    @Test
    @DisplayName("상세 조회 — 존재하지 않는 루트")
    void get_detail_not_found() {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> routeQueryService.getDetail(publicId, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_NOT_FOUND);
    }

    @Test
    @DisplayName("상세 조회 — 소유자가 아니면 실패")
    void get_detail_forbidden() {
        // given
        UUID publicId = UUID.randomUUID();
        Route route = RouteFixture.createRoute(
                MEMBER_ID, "Retro mood trip", START, START,
                List.of(RouteFixture.createDay(1, START, 1))
        );

        given(routeRepository.findByPublicId(publicId))
                .willReturn(Optional.of(route));

        UUID otherMemberId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> routeQueryService.getDetail(publicId, otherMemberId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_FORBIDDEN);
    }

    @Test
    @DisplayName("목록 조회 — 첫 페이지 성공")
    void get_list_first_page() {
        // given
        int size = 2;
        LocalDateTime now = LocalDateTime.now();

        List<RouteListRow> rows = List.of(
                new RouteListRow(3L, UUID.randomUUID(), "Busan coastal walk", START, END, now),
                new RouteListRow(2L, UUID.randomUUID(), "Seoul art tour", START, END, now.minusHours(1)),
                new RouteListRow(1L, UUID.randomUUID(), "Jeju nature trip", START, END, now.minusHours(2))
        );

        given(routeQueryRepository.findByMemberLatest(MEMBER_ID, null, null, size))
                .willReturn(rows);

        // when
        CursorResponse<RouteListRow> result = routeQueryService.getList(MEMBER_ID, null, size);

        // then
        assertThat(result.items()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isNotNull();
    }

    @Test
    @DisplayName("목록 조회 — 마지막 페이지")
    void get_list_last_page() {
        // given
        int size = 10;
        LocalDateTime now = LocalDateTime.now();

        List<RouteListRow> rows = List.of(
                new RouteListRow(1L, UUID.randomUUID(), "My only route", START, END, now)
        );

        given(routeQueryRepository.findByMemberLatest(MEMBER_ID, null, null, size))
                .willReturn(rows);

        // when
        CursorResponse<RouteListRow> result = routeQueryService.getList(MEMBER_ID, null, size);

        // then
        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("목록 조회 — 빈 결과")
    void get_list_empty() {
        // given
        given(routeQueryRepository.findByMemberLatest(MEMBER_ID, null, null, 20))
                .willReturn(List.of());

        // when
        CursorResponse<RouteListRow> result = routeQueryService.getList(MEMBER_ID, null, 20);

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }
}
