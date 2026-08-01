package com.moodi.route.application;

import com.moodi.spot.domain.SpotContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertThat(result.get(0)).hasSize(3);
        assertThat(result.get(1)).hasSize(2);
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
    }

    @Test
    @DisplayName("경도 기준으로 서→동 분산")
    void distribute_by_longitude() {
        SpotSnapshot east = createSnapshot(1L, 37.55, 129.0);
        SpotSnapshot west = createSnapshot(2L, 37.55, 126.0);
        SpotSnapshot mid = createSnapshot(3L, 37.55, 127.5);
        SpotSnapshot farEast = createSnapshot(4L, 37.55, 130.0);

        List<List<SpotSnapshot>> result = distributor.distribute(
                List.of(east, west, mid, farEast), 2);

        // Day 1: west, mid (서쪽)  Day 2: east, farEast (동쪽)
        assertThat(result.get(0).stream().map(SpotSnapshot::spotId))
                .containsExactly(2L, 3L);
        assertThat(result.get(1).stream().map(SpotSnapshot::spotId))
                .containsExactly(1L, 4L);
    }

    private SpotSnapshot createSnapshot(Long spotId, double lat, double lng) {
        return new SpotSnapshot(
                spotId, "스팟 " + spotId, "https://img.example.com/" + spotId + ".jpg",
                "서울", "성동구", lat, lng,
                SpotContentType.TOURIST_ATTRACTION, null
        );
    }
}
