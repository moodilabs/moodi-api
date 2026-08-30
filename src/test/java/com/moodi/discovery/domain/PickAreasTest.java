package com.moodi.discovery.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PickAreasTest {

    private static final PickArea SEOUL = new PickArea(PickAreaLevel.REGION, "서울", null, null);
    private static final PickArea SEONGDONG =
            new PickArea(PickAreaLevel.DISTRICT, "서울", "성동구", null);
    private static final PickArea SEONGSU =
            new PickArea(PickAreaLevel.NEIGHBORHOOD, "서울", "성동구", "성수동");
    private static final PickArea BUSAN = new PickArea(PickAreaLevel.REGION, "부산", null, null);

    @Test
    @DisplayName("지역을 하나도 고르지 않으면 추천할 수 없다")
    void of_rejects_empty() {
        assertThatThrownBy(() -> PickAreas.of(List.of()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICK_INVALID_AREA_SELECTION);
    }

    @Test
    @DisplayName("지역은 5개까지만 고를 수 있다")
    void of_rejects_over_max_size() {
        List<PickArea> six = IntStream.range(0, 6)
                .mapToObj(i -> new PickArea(PickAreaLevel.REGION, "지역" + i, null, null))
                .toList();

        assertThatThrownBy(() -> PickAreas.of(six))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICK_INVALID_AREA_SELECTION);
    }

    @Test
    @DisplayName("정확히 5개는 고를 수 있다")
    void of_allows_max_size() {
        List<PickArea> five = IntStream.range(0, 5)
                .mapToObj(i -> new PickArea(PickAreaLevel.REGION, "지역" + i, null, null))
                .toList();

        assertThat(PickAreas.of(five).size()).isEqualTo(5);
    }

    @Test
    @DisplayName("상위 지역을 함께 고르면 하위 지역은 접히고 상위만 남는다")
    void of_collapses_narrower_area_into_wider() {
        PickAreas areas = PickAreas.of(List.of(SEONGSU, SEOUL));

        assertThat(areas.values()).containsExactly(SEOUL);
    }

    @Test
    @DisplayName("여러 단계가 겹쳐도 가장 넓은 지역 하나만 남는다")
    void of_collapses_through_multiple_levels() {
        PickAreas areas = PickAreas.of(List.of(SEONGSU, SEONGDONG, SEOUL));

        assertThat(areas.values()).containsExactly(SEOUL);
    }

    @Test
    @DisplayName("같은 지역을 두 번 골라도 하나로 접힌다")
    void of_removes_duplicates() {
        PickAreas areas = PickAreas.of(List.of(SEOUL, SEOUL));

        assertThat(areas.values()).containsExactly(SEOUL);
    }

    @Test
    @DisplayName("포함 관계가 없는 지역은 그대로 유지된다")
    void of_keeps_unrelated_areas() {
        PickAreas areas = PickAreas.of(List.of(SEONGSU, BUSAN));

        assertThat(areas.values()).containsExactlyInAnyOrder(SEONGSU, BUSAN);
    }

    @Test
    @DisplayName("같은 시·도라도 구가 다르면 서로를 포함하지 않는다")
    void of_keeps_sibling_districts() {
        PickArea mapo = new PickArea(PickAreaLevel.DISTRICT, "서울", "마포구", null);

        PickAreas areas = PickAreas.of(List.of(SEONGDONG, mapo));

        assertThat(areas.values()).containsExactlyInAnyOrder(SEONGDONG, mapo);
    }

    @Test
    @DisplayName("구 단위인데 구가 비어 있으면 잘못된 선택이다")
    void area_rejects_missing_district() {
        assertThatThrownBy(() -> new PickArea(PickAreaLevel.DISTRICT, "서울", null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICK_INVALID_AREA_SELECTION);
    }

    @Test
    @DisplayName("시·도 단위인데 하위 값이 채워져 있으면 잘못된 선택이다")
    void area_rejects_unexpected_district() {
        assertThatThrownBy(() -> new PickArea(PickAreaLevel.REGION, "서울", "성동구", null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICK_INVALID_AREA_SELECTION);
    }
}
