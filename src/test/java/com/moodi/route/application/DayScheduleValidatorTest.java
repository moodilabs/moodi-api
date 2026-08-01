package com.moodi.route.application;

import com.moodi.route.domain.TravelMode;
import com.moodi.spot.domain.SpotContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DayScheduleValidatorTest {

    private final DayScheduleValidator validator = new DayScheduleValidator();

    @Test
    @DisplayName("8시간 이내면 그대로 반환")
    void within_limit_returns_unchanged() {
        // 관광지 120분 × 2 = 240분 체류 + 이동 30분 = 270분 < 480분
        List<List<SpotSnapshot>> distribution = List.of(
                new ArrayList<>(List.of(
                        createSnapshot(1L, SpotContentType.TOURIST_ATTRACTION),
                        createSnapshot(2L, SpotContentType.TOURIST_ATTRACTION)
                ))
        );
        List<List<LegResult>> legs = List.of(
                List.of(new LegResult(TravelMode.WALK, 1800, 1500, null))
        );

        List<List<SpotSnapshot>> result = validator.validateAndRebalance(distribution, legs);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(2);
    }

    @Test
    @DisplayName("8시간 초과하면 마지막 스팟을 다음 날로 이동")
    void overloaded_day_moves_spot_to_next_day() {
        // Day 1: 관광지 120 × 4 = 480분 + 이동 30분 = 510분 > 480분
        // Day 2: 빈 날
        List<List<SpotSnapshot>> distribution = List.of(
                new ArrayList<>(List.of(
                        createSnapshot(1L, SpotContentType.TOURIST_ATTRACTION),
                        createSnapshot(2L, SpotContentType.TOURIST_ATTRACTION),
                        createSnapshot(3L, SpotContentType.TOURIST_ATTRACTION),
                        createSnapshot(4L, SpotContentType.TOURIST_ATTRACTION)
                )),
                new ArrayList<>(List.of())
        );
        List<List<LegResult>> legs = List.of(
                List.of(
                        new LegResult(TravelMode.WALK, 600, 500, null),
                        new LegResult(TravelMode.WALK, 600, 500, null),
                        new LegResult(TravelMode.WALK, 600, 500, null)
                ),
                List.of()
        );

        List<List<SpotSnapshot>> result = validator.validateAndRebalance(distribution, legs);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).size()).isLessThan(4);
        assertThat(result.get(1)).isNotEmpty();
    }

    @Test
    @DisplayName("마지막 날짜가 초과하면 재조정 불가 → 빈 리스트")
    void last_day_overloaded_returns_empty() {
        // 날짜 1개, 관광지 5개 = 600분 > 480분
        List<List<SpotSnapshot>> distribution = List.of(
                new ArrayList<>(List.of(
                        createSnapshot(1L, SpotContentType.TOURIST_ATTRACTION),
                        createSnapshot(2L, SpotContentType.TOURIST_ATTRACTION),
                        createSnapshot(3L, SpotContentType.TOURIST_ATTRACTION),
                        createSnapshot(4L, SpotContentType.TOURIST_ATTRACTION),
                        createSnapshot(5L, SpotContentType.TOURIST_ATTRACTION)
                ))
        );
        List<List<LegResult>> legs = List.of(List.of());

        List<List<SpotSnapshot>> result = validator.validateAndRebalance(distribution, legs);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("하루 총 시간 계산")
    void calculate_day_minutes() {
        List<SpotSnapshot> spots = List.of(
                createSnapshot(1L, SpotContentType.TOURIST_ATTRACTION),  // 120분
                createSnapshot(2L, SpotContentType.CULTURAL_FACILITY)    // 90분
        );
        List<LegResult> legs = List.of(
                new LegResult(TravelMode.WALK, 1800, 1500, null)  // 30분
        );

        int total = validator.calculateDayMinutes(spots, legs);

        assertThat(total).isEqualTo(240); // 120 + 90 + 30
    }

    private SpotSnapshot createSnapshot(Long spotId, SpotContentType contentType) {
        return new SpotSnapshot(
                spotId, "스팟 " + spotId, null,
                "서울", "성동구", 37.55, 127.05,
                contentType, null
        );
    }
}
