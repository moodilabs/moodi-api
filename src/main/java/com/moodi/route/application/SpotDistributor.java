package com.moodi.route.application;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class SpotDistributor {

    /**
     * 스팟을 좌표 기반으로 날짜별 분산한다.
     * 1. 경도 기준 정렬 (서→동 이동 가정)
     * 2. 날짜별 균등 분배
     * 3. 각 날짜 내에서 가장 가까운 스팟 순서로 정렬 (Nearest Neighbor)
     */
    public List<List<SpotSnapshot>> distribute(List<SpotSnapshot> spots, int totalDays) {
        List<SpotSnapshot> sorted = spots.stream()
                .sorted(Comparator.comparingDouble(SpotSnapshot::longitude))
                .toList();

        List<List<SpotSnapshot>> result = new ArrayList<>();
        int spotCount = sorted.size();
        int baseSize = spotCount / totalDays;
        int remainder = spotCount % totalDays;

        int index = 0;
        for (int day = 0; day < totalDays; day++) {
            int daySize = baseSize + (day < remainder ? 1 : 0);
            List<SpotSnapshot> daySpots = new ArrayList<>(sorted.subList(index, index + daySize));
            result.add(orderByNearestNeighbor(daySpots));
            index += daySize;
        }

        return result;
    }

    /**
     * Nearest Neighbor 알고리즘으로 방문 순서 결정.
     * 첫 스팟부터 시작하여 가장 가까운 다음 스팟을 선택한다.
     */
    List<SpotSnapshot> orderByNearestNeighbor(List<SpotSnapshot> spots) {
        if (spots.size() <= 1) {
            return spots;
        }

        List<SpotSnapshot> remaining = new ArrayList<>(spots);
        List<SpotSnapshot> ordered = new ArrayList<>();

        SpotSnapshot current = remaining.removeFirst();
        ordered.add(current);

        while (!remaining.isEmpty()) {
            SpotSnapshot nearest = findNearest(current, remaining);
            remaining.remove(nearest);
            ordered.add(nearest);
            current = nearest;
        }

        return ordered;
    }

    private SpotSnapshot findNearest(SpotSnapshot from, List<SpotSnapshot> candidates) {
        SpotSnapshot nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (SpotSnapshot candidate : candidates) {
            double distance = calculateDistance(
                    from.latitude(), from.longitude(),
                    candidate.latitude(), candidate.longitude()
            );
            if (distance < minDistance) {
                minDistance = distance;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        return dLat * dLat + dLon * dLon;
    }
}
