package com.moodi.route.application;

import com.moodi.route.domain.StayDurationPolicy;
import com.moodi.spot.domain.SpotContentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DayScheduleValidator {

    private static final int MAX_DAILY_MINUTES = 480; // 8시간

    /**
     * 각 날짜의 체류시간 + 이동시간이 8시간 이내인지 검증한다.
     * 초과하는 날짜가 있으면 마지막 스팟을 다음 날짜로 이동시킨다.
     *
     * @return 재조정된 스팟 분배. 재조정 불가능하면 빈 Optional.
     */
    public List<List<SpotSnapshot>> validateAndRebalance(
            List<List<SpotSnapshot>> distribution,
            List<List<LegResult>> legsByDay) {

        List<List<SpotSnapshot>> result = new ArrayList<>();
        for (List<SpotSnapshot> daySpots : distribution) {
            result.add(new ArrayList<>(daySpots));
        }

        int maxIterations = result.size() * 2;
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            int overloadedDay = findOverloadedDay(result, legsByDay);
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

            SpotSnapshot moved = from.removeLast();
            result.get(overloadedDay + 1).addFirst(moved);
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
}
