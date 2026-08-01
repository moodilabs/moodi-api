package com.moodi.route.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.route.support.RouteFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteDayTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 10);

    @Test
    @DisplayName("하루 최대 6개 스팟 생성 성공")
    void create_day_with_max_spots() {
        RouteDay day = RouteFixture.createDay(1, DATE, 6);

        assertThat(day.getSpotCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("하루 7개 스팟이면 실패")
    void create_day_exceeding_spot_limit() {
        List<RouteSpot> spots = new java.util.ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            spots.add(RouteFixture.createSpot(i));
        }

        assertThatThrownBy(() -> RouteDay.create(1, DATE, spots, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_DAY_SPOT_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("스팟 순서(sequence)가 중복되면 실패")
    void create_day_duplicate_spot_sequence() {
        List<RouteSpot> spots = List.of(
                RouteFixture.createSpot(1),
                RouteFixture.createSpot(1)
        );

        assertThatThrownBy(() -> RouteDay.create(1, DATE, spots, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_DUPLICATE_SPOT_SEQUENCE);
    }

    @Test
    @DisplayName("빈 스팟 목록으로 생성 가능")
    void create_day_with_no_spots() {
        RouteDay day = RouteDay.create(1, DATE, List.of(), List.of());

        assertThat(day.getSpotCount()).isZero();
    }
}
