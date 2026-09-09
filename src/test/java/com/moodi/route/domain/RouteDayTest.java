package com.moodi.route.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.route.support.RouteFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteDayTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 10);

    @Test
    @DisplayName("스팟 개수 제한 없이 생성 성공")
    void create_day_with_many_spots() {
        RouteDay day = RouteFixture.createDay(1, DATE, 8);

        assertThat(day.getSpotCount()).isEqualTo(8);
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

    @Test
    @DisplayName("기존 Day에 스팟과 이동정보를 덧붙인다")
    void add_spot_and_leg_to_existing_day() {
        RouteDay day = RouteFixture.createDay(1, DATE, 2);
        int nextSequence = day.getNextSequence();

        day.addSpot(RouteFixture.createSpot(nextSequence));
        day.addLeg(RouteFixture.createLeg(2, nextSequence));

        assertThat(nextSequence).isEqualTo(3);
        assertThat(day.getSpotCount()).isEqualTo(3);
        assertThat(day.getLastSpot()).isPresent()
                .get().extracting(RouteSpot::getSequence).isEqualTo(3);
        assertThat(day.getLegs()).hasSize(2);
        assertThat(day.getLegs().get(1).getToSequence()).isEqualTo(3);
    }

    @Test
    @DisplayName("이미 있는 순서(sequence)의 스팟을 덧붙이면 실패")
    void add_spot_duplicate_sequence() {
        RouteDay day = RouteFixture.createDay(1, DATE, 2);

        assertThatThrownBy(() -> day.addSpot(RouteFixture.createSpot(2)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTE_DUPLICATE_SPOT_SEQUENCE);
        assertThat(day.getSpotCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("스팟이 없는 Day는 마지막 스팟이 없고 다음 순서는 1이다")
    void empty_day_has_no_last_spot() {
        RouteDay day = RouteDay.create(1, DATE, new ArrayList<>(), new ArrayList<>());

        assertThat(day.getLastSpot()).isEmpty();
        assertThat(day.getNextSequence()).isEqualTo(1);
    }
}
