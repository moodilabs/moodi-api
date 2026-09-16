package com.moodi.route.domain;

import com.moodi.shared.error.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class RecommendedRouteTest {
    private RecommendedRoute create(List<RecommendedRouteStop> stops) {
        return RecommendedRoute.create("Walk", "https://example.com/a.jpg", "Seoul", true, 0, stops);
    }

    @Test @DisplayName("운영 루트는 일차와 스팟 순서를 정렬한다")
    void sorts_valid_schedule() {
        RecommendedRoute route = create(List.of(new RecommendedRouteStop(3L, 2, 1),
                new RecommendedRouteStop(2L, 1, 2), new RecommendedRouteStop(1L, 1, 1)));
        assertThat(route.getStops()).extracting(RecommendedRouteStop::getSpotId).containsExactly(1L, 2L, 3L);
    }

    @Test @DisplayName("운영 루트는 중복·누락 순서와 허용 일차 초과를 거부한다")
    void rejects_invalid_schedules() {
        List<List<RecommendedRouteStop>> invalid = List.of(
                List.of(),
                List.of(new RecommendedRouteStop(1L, 2, 1)),
                List.of(new RecommendedRouteStop(1L, 1, 2)),
                List.of(new RecommendedRouteStop(1L, 1, 1), new RecommendedRouteStop(1L, 2, 1)),
                List.of(new RecommendedRouteStop(1L, 1, 1), new RecommendedRouteStop(2L, 1, 1)),
                List.of(new RecommendedRouteStop(1L, 6, 1)),
                List.of(new RecommendedRouteStop(1L, 1, 7)));
        for (List<RecommendedRouteStop> stops : invalid) {
            assertThatThrownBy(() -> create(stops)).isInstanceOf(BusinessException.class);
        }
    }
}
