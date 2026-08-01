package com.moodi.route.application;

import com.moodi.route.domain.StayDurationPolicy;
import com.moodi.route.domain.TravelMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DayScheduleValidator {

    private static final int MAX_DAILY_MINUTES = 480; // 8시간
    private static final int MAX_SPOTS_PER_DAY = 6;

    private final LegCalculator legCalculator;
    private final SpotDistributor spotDistributor;

    /**
     * 각 날짜의 체류시간 + 이동시간이 8시간 이내인지 검증한다.
     * 초과하는 날짜가 있으면 마지막 스팟을 다음 날짜로 이동시킨다.
     *
     * @return 재조정된 스팟 분배. 재조정 불가능하면 빈 리스트.
     */
    public List<List<SpotSnapshot>> validateAndRebalance(
            List<List<SpotSnapshot>> distribution,
            List<List<LegResult>> legsByDay) {

        List<List<SpotSnapshot>> result = new ArrayList<>();
        for (List<SpotSnapshot> daySpots : distribution) {
            result.add(new ArrayList<>(daySpots));
        }

        List<List<LegResult>> currentLegs = new ArrayList<>(legsByDay);

        int maxIterations = result.size() * 2;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            int overloadedDay = findOverloadedDay(result, currentLegs);
            if (overloadedDay == -1) {
                return result;
            }

            if (overloadedDay >= result.size() - 1) {
                return List.of(); // 마지막 날짜가 초과하면 재조정 불가
            }

            List<SpotSnapshot> from = result.get(overloadedDay);
            if (from.size() <= 1) {
                return List.of(); // 스팟 1개인데 초과하면 재조정 불가
            }

            List<SpotSnapshot> to = result.get(overloadedDay + 1);
            if (to.size() >= MAX_SPOTS_PER_DAY) {
                return List.of(); // 이동 대상 날짜가 이미 최대 스팟 수에 도달
            }

            SpotSnapshot moved = from.removeLast();
            to.add(moved);

            // 영향받은 날짜들의 방문 순서를 Nearest Neighbor로 재계산
            result.set(overloadedDay, new ArrayList<>(spotDistributor.orderByNearestNeighbor(from)));
            result.set(overloadedDay + 1, new ArrayList<>(spotDistributor.orderByNearestNeighbor(to)));

            // 영향받은 날짜들의 구간(Leg)을 재계산
            currentLegs.set(overloadedDay, calculateLegsForDay(result.get(overloadedDay)));
            if (overloadedDay + 1 < currentLegs.size()) {
                currentLegs.set(overloadedDay + 1, calculateLegsForDay(result.get(overloadedDay + 1)));
            } else {
                currentLegs.add(calculateLegsForDay(result.get(overloadedDay + 1)));
            }
        }

        return List.of(); // 반복 초과
    }

    public int calculateDayMinutes(List<SpotSnapshot> spots, List<LegResult> legs) {
        int stayMinutes = spots.stream()
                .mapToInt(spot -> StayDurationPolicy.getEstimatedMinutes(spot.contentType()))
                .sum();

        int travelMinutes = legs.stream()
                .mapToInt(leg -> leg.durationSeconds() / 60)
                .sum();

        return stayMinutes + travelMinutes;
    }

    private int findOverloadedDay(List<List<SpotSnapshot>> distribution,
                                   List<List<LegResult>> legsByDay) {
        for (int i = 0; i < distribution.size(); i++) {
            List<LegResult> dayLegs = i < legsByDay.size() ? legsByDay.get(i) : List.of();
            int totalMinutes = calculateDayMinutes(distribution.get(i), dayLegs);
            if (totalMinutes > MAX_DAILY_MINUTES) {
                return i;
            }
        }
        return -1;
    }

    private List<LegResult> calculateLegsForDay(List<SpotSnapshot> spots) {
        List<LegResult> legs = new ArrayList<>();
        for (int i = 0; i < spots.size() - 1; i++) {
            SpotSnapshot from = spots.get(i);
            SpotSnapshot to = spots.get(i + 1);
            Optional<LegResult> legResult = legCalculator.calculate(
                    from.longitude(), from.latitude(),
                    to.longitude(), to.latitude()
            );
            legs.add(legResult.orElse(
                    new LegResult(TravelMode.WALK, 0, 0, null)
            ));
        }
        return legs;
    }
}
