package com.moodi.route.application;

import com.moodi.route.domain.RouteSpotType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SpotDistributorTest {

    private final SpotDistributor distributor = new SpotDistributor();

    @Test
    @DisplayName("4개 스팟을 2일에 균등 분배")
    void distribute_evenly() {
        List<SpotSnapshot> spots = List.of(
                createSnapshot(1L, 37.55, 127.05),
                createSnapshot(2L, 37.56, 127.06),
                createSnapshot(3L, 37.57, 127.07),
                createSnapshot(4L, 37.58, 127.08)
        );

        List<List<SpotSnapshot>> result = distributor.distribute(spots, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).hasSize(2);
        assertThat(result.get(1)).hasSize(2);
    }

    @Test
    @DisplayName("5개 스팟을 2일에 분배하면 3+2")
    void distribute_unevenly() {
        List<SpotSnapshot> spots = List.of(
                createSnapshot(1L, 37.55, 127.01),
                createSnapshot(2L, 37.56, 127.02),
                createSnapshot(3L, 37.57, 127.03),
                createSnapshot(4L, 37.58, 127.04),
                createSnapshot(5L, 37.59, 127.05)
        );

        List<List<SpotSnapshot>> result = distributor.distribute(spots, 2);

        assertThat(result).hasSize(2);
        int day1 = result.get(0).size();
        int day2 = result.get(1).size();
        assertThat(day1 + day2).isEqualTo(5);
        assertThat(Math.abs(day1 - day2)).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("1개 스팟을 1일에 분배")
    void distribute_single_spot() {
        List<SpotSnapshot> spots = List.of(createSnapshot(1L, 37.55, 127.05));

        List<List<SpotSnapshot>> result = distributor.distribute(spots, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(1);
    }

    @Test
    @DisplayName("스팟보다 날짜가 많으면 빈 날짜 존재")
    void distribute_more_days_than_spots() {
        List<SpotSnapshot> spots = List.of(
                createSnapshot(1L, 37.55, 127.05),
                createSnapshot(2L, 37.56, 127.06)
        );

        List<List<SpotSnapshot>> result = distributor.distribute(spots, 3);

        assertThat(result).hasSize(3);
        long nonEmpty = result.stream().filter(day -> !day.isEmpty()).count();
        assertThat(nonEmpty).isEqualTo(2);
        int totalSpots = result.stream().mapToInt(List::size).sum();
        assertThat(totalSpots).isEqualTo(2);
    }

    @Test
    @DisplayName("경도만 다른 스팟은 서→동 순서로 Day에 매핑")
    void distribute_by_longitude() {
        SpotSnapshot east = createSnapshot(1L, 37.55, 129.0);
        SpotSnapshot west = createSnapshot(2L, 37.55, 126.0);
        SpotSnapshot mid = createSnapshot(3L, 37.55, 127.5);
        SpotSnapshot farEast = createSnapshot(4L, 37.55, 130.0);

        List<List<SpotSnapshot>> result = distributor.distribute(
                List.of(east, west, mid, farEast), 2);

        Set<Long> day1Ids = result.get(0).stream().map(SpotSnapshot::spotId).collect(Collectors.toSet());
        Set<Long> day2Ids = result.get(1).stream().map(SpotSnapshot::spotId).collect(Collectors.toSet());

        // 서쪽(126.0, 127.5)과 동쪽(129.0, 130.0)이 다른 날에 분리
        assertThat(day1Ids).containsExactlyInAnyOrder(2L, 3L);
        assertThat(day2Ids).containsExactlyInAnyOrder(1L, 4L);
    }

    @Test
    @DisplayName("경도는 비슷하지만 위도 차이가 큰 스팟이 다른 날에 분리")
    void distribute_separates_spots_with_large_latitude_gap() {
        // 강남권 (위도 ~37.50)
        SpotSnapshot gangnam1 = createSnapshot(1L, 37.497, 127.027);
        SpotSnapshot gangnam2 = createSnapshot(2L, 37.510, 127.040);
        // 동대문권 (위도 ~37.57) - 경도는 강남과 비슷
        SpotSnapshot dongdaemun1 = createSnapshot(3L, 37.571, 127.009);
        SpotSnapshot dongdaemun2 = createSnapshot(4L, 37.580, 127.049);

        List<List<SpotSnapshot>> result = distributor.distribute(
                List.of(gangnam1, gangnam2, dongdaemun1, dongdaemun2), 2);

        Set<Long> day1Ids = result.get(0).stream().map(SpotSnapshot::spotId).collect(Collectors.toSet());
        Set<Long> day2Ids = result.get(1).stream().map(SpotSnapshot::spotId).collect(Collectors.toSet());

        // 강남권과 동대문권이 같은 날에 섞이지 않고 분리
        boolean separated = (day1Ids.containsAll(Set.of(1L, 2L)) && day2Ids.containsAll(Set.of(3L, 4L)))
                || (day1Ids.containsAll(Set.of(3L, 4L)) && day2Ids.containsAll(Set.of(1L, 2L)));
        assertThat(separated).isTrue();
    }

    @Test
    @DisplayName("종로권과 강남권 스팟이 자연스럽게 클러스터링")
    void distribute_clusters_nearby_spots_together() {
        // 종로권
        SpotSnapshot jongro1 = createSnapshot(1L, 37.572, 126.977);
        SpotSnapshot jongro2 = createSnapshot(2L, 37.579, 126.985);
        SpotSnapshot jongro3 = createSnapshot(3L, 37.575, 126.991);
        // 강남권
        SpotSnapshot gangnam1 = createSnapshot(4L, 37.497, 127.027);
        SpotSnapshot gangnam2 = createSnapshot(5L, 37.504, 127.024);
        SpotSnapshot gangnam3 = createSnapshot(6L, 37.510, 127.040);

        List<List<SpotSnapshot>> result = distributor.distribute(
                List.of(jongro1, gangnam1, jongro2, gangnam2, jongro3, gangnam3), 2);

        Set<Long> day1Ids = result.get(0).stream().map(SpotSnapshot::spotId).collect(Collectors.toSet());
        Set<Long> day2Ids = result.get(1).stream().map(SpotSnapshot::spotId).collect(Collectors.toSet());

        // 종로(1,2,3)와 강남(4,5,6)이 각각 같은 날에 묶임
        boolean clustered = (day1Ids.containsAll(Set.of(1L, 2L, 3L)) && day2Ids.containsAll(Set.of(4L, 5L, 6L)))
                || (day1Ids.containsAll(Set.of(4L, 5L, 6L)) && day2Ids.containsAll(Set.of(1L, 2L, 3L)));
        assertThat(clustered).isTrue();
    }

    @Test
    @DisplayName("10개 스팟 / 3일 분배 시 날짜별 개수 차이가 1 이하")
    void distribute_balanced_10_spots_3_days() {
        List<SpotSnapshot> spots = List.of(
                createSnapshot(1L, 37.50, 126.90),
                createSnapshot(2L, 37.51, 126.92),
                createSnapshot(3L, 37.52, 126.94),
                createSnapshot(4L, 37.53, 126.96),
                createSnapshot(5L, 37.55, 127.00),
                createSnapshot(6L, 37.56, 127.02),
                createSnapshot(7L, 37.57, 127.04),
                createSnapshot(8L, 37.60, 127.10),
                createSnapshot(9L, 37.61, 127.12),
                createSnapshot(10L, 37.62, 127.14)
        );

        List<List<SpotSnapshot>> result = distributor.distribute(spots, 3);

        assertThat(result).hasSize(3);
        int totalSpots = result.stream().mapToInt(List::size).sum();
        assertThat(totalSpots).isEqualTo(10);

        int min = result.stream().mapToInt(List::size).min().orElse(0);
        int max = result.stream().mapToInt(List::size).max().orElse(0);
        assertThat(max - min).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("11개 스팟 / 3일 분배 시 날짜별 개수 차이가 1 이하")
    void distribute_balanced_11_spots_3_days() {
        List<SpotSnapshot> spots = List.of(
                createSnapshot(1L, 37.50, 126.90),
                createSnapshot(2L, 37.51, 126.92),
                createSnapshot(3L, 37.52, 126.94),
                createSnapshot(4L, 37.53, 126.96),
                createSnapshot(5L, 37.54, 126.98),
                createSnapshot(6L, 37.55, 127.00),
                createSnapshot(7L, 37.56, 127.02),
                createSnapshot(8L, 37.57, 127.04),
                createSnapshot(9L, 37.60, 127.10),
                createSnapshot(10L, 37.61, 127.12),
                createSnapshot(11L, 37.62, 127.14)
        );

        List<List<SpotSnapshot>> result = distributor.distribute(spots, 3);

        assertThat(result).hasSize(3);
        int totalSpots = result.stream().mapToInt(List::size).sum();
        assertThat(totalSpots).isEqualTo(11);

        int min = result.stream().mapToInt(List::size).min().orElse(0);
        int max = result.stream().mapToInt(List::size).max().orElse(0);
        assertThat(max - min).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("같은 입력에서 결과가 항상 동일 (deterministic)")
    void distribute_deterministic() {
        List<SpotSnapshot> spots = List.of(
                createSnapshot(1L, 37.497, 127.027),
                createSnapshot(2L, 37.571, 127.009),
                createSnapshot(3L, 37.510, 127.040),
                createSnapshot(4L, 37.580, 127.049),
                createSnapshot(5L, 37.555, 126.970),
                createSnapshot(6L, 37.520, 127.020)
        );

        List<List<SpotSnapshot>> first = distributor.distribute(spots, 2);

        for (int i = 0; i < 10; i++) {
            List<List<SpotSnapshot>> result = distributor.distribute(spots, 2);
            for (int day = 0; day < result.size(); day++) {
                List<Long> expectedIds = first.get(day).stream().map(SpotSnapshot::spotId).toList();
                List<Long> actualIds = result.get(day).stream().map(SpotSnapshot::spotId).toList();
                assertThat(actualIds).isEqualTo(expectedIds);
            }
        }
    }

    @Test
    @DisplayName("모든 스팟이 같은 위치에 있으면 균등 분배만 수행")
    void distribute_all_spots_same_location() {
        List<SpotSnapshot> spots = List.of(
                createSnapshot(1L, 37.55, 127.05),
                createSnapshot(2L, 37.55, 127.05),
                createSnapshot(3L, 37.55, 127.05),
                createSnapshot(4L, 37.55, 127.05)
        );

        List<List<SpotSnapshot>> result = distributor.distribute(spots, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).hasSize(2);
        assertThat(result.get(1)).hasSize(2);
    }

    @Test
    @DisplayName("빈 스팟 리스트를 분배하면 모든 날짜가 빈 리스트")
    void distribute_empty_spots() {
        List<List<SpotSnapshot>> result = distributor.distribute(List.of(), 3);

        assertThat(result).hasSize(3);
        result.forEach(day -> assertThat(day).isEmpty());
    }

    private SpotSnapshot createSnapshot(Long spotId, double lat, double lng) {
        return new SpotSnapshot(
                spotId, "스팟 " + spotId, "https://img.example.com/" + spotId + ".jpg",
                "서울", "성동구", lat, lng,
                RouteSpotType.TOURIST_ATTRACTION, null
        );
    }
}
