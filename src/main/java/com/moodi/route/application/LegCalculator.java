package com.moodi.route.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegCalculator {

    private static final double WALK_THRESHOLD_METERS = 2_000;

    private final LegClient legClient;

    public Optional<LegResult> calculate(double startLongitude, double startLatitude,
                                          double endLongitude, double endLatitude) {
        double straightDistance = calculateStraightDistance(startLatitude, startLongitude,
                endLatitude, endLongitude);

        if (straightDistance < WALK_THRESHOLD_METERS) {
            return findWalkRoute(startLongitude, startLatitude, endLongitude, endLatitude);
        }

        Optional<LegResult> transit = legClient.findTransitRoute(
                startLongitude, startLatitude, endLongitude, endLatitude);

        if (transit.isPresent()) {
            return transit;
        }

        log.info("대중교통 경로 없음, 도보 fallback: ({}, {}) → ({}, {})",
                startLongitude, startLatitude, endLongitude, endLatitude);
        return findWalkRoute(startLongitude, startLatitude, endLongitude, endLatitude);
    }

    private Optional<LegResult> findWalkRoute(double startLongitude, double startLatitude,
                                               double endLongitude, double endLatitude) {
        return legClient.findWalkRoute(startLongitude, startLatitude, endLongitude, endLatitude);
    }

    /**
     * Haversine 공식으로 두 좌표 간 직선거리(미터) 계산
     */
    static double calculateStraightDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
