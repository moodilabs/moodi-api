package com.moodi.route.application;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class SpotDistributor {

    /**
     * 스팟을 2D 좌표 기반으로 날짜별 분산한다.
     * 1. 날짜별 목표 스팟 수(capacity) 계산
     * 2. Farthest-First로 초기 클러스터 중심 선정
     * 3. Regret 기반 Greedy 할당 (capacity 제약)
     * 4. 클러스터를 경도순으로 Day에 매핑 (deterministic 순서 보장용)
     * 5. 각 날짜 내에서 Nearest Neighbor로 방문 순서 결정
     */
    public List<List<SpotSnapshot>> distribute(List<SpotSnapshot> spots, int totalDays) {
        if (spots.isEmpty()) {
            return createEmptyDays(totalDays);
        }

        int clusterCount = Math.min(spots.size(), totalDays);
        int[] capacities = calculateCapacities(spots.size(), clusterCount);

        List<SpotSnapshot> centers = selectInitialCenters(spots, clusterCount);
        List<List<SpotSnapshot>> clusters = assignToClusters(spots, centers, capacities);
        sortClustersByLongitude(clusters);

        List<List<SpotSnapshot>> result = new ArrayList<>();
        for (int day = 0; day < totalDays; day++) {
            if (day < clusters.size()) {
                result.add(orderByNearestNeighbor(clusters.get(day)));
            } else {
                result.add(new ArrayList<>());
            }
        }
        return result;
    }

    private List<List<SpotSnapshot>> createEmptyDays(int totalDays) {
        List<List<SpotSnapshot>> result = new ArrayList<>();
        for (int i = 0; i < totalDays; i++) {
            result.add(new ArrayList<>());
        }
        return result;
    }

    /**
     * 클러스터별 목표 스팟 수를 계산한다.
     * 예: 9/3 → [3,3,3], 10/3 → [4,3,3], 2/2 → [1,1]
     */
    private int[] calculateCapacities(int spotCount, int clusterCount) {
        int[] capacities = new int[clusterCount];
        int base = spotCount / clusterCount;
        int remainder = spotCount % clusterCount;
        for (int i = 0; i < clusterCount; i++) {
            capacities[i] = base + (i < remainder ? 1 : 0);
        }
        return capacities;
    }

    /**
     * Farthest-First Traversal로 초기 클러스터 중심을 선정한다.
     * 1. 전체 스팟의 무게중심(centroid)에서 가장 먼 스팟을 첫 번째 중심으로 선택
     * 2. 이후 기존 중심들과의 최소 거리가 가장 큰 스팟을 다음 중심으로 선택
     * 거리 동률 시 spotId가 작은 것을 선택하여 deterministic 보장.
     */
    private List<SpotSnapshot> selectInitialCenters(List<SpotSnapshot> spots, int k) {
        double centroidLat = spots.stream().mapToDouble(SpotSnapshot::latitude).average().orElse(0);
        double centroidLon = spots.stream().mapToDouble(SpotSnapshot::longitude).average().orElse(0);

        List<SpotSnapshot> centers = new ArrayList<>();

        SpotSnapshot first = null;
        double maxDist = -1;
        for (SpotSnapshot spot : spots) {
            double dist = calculateDistance(spot.latitude(), spot.longitude(), centroidLat, centroidLon);
            if (dist > maxDist || (dist == maxDist && (first == null || spot.spotId() < first.spotId()))) {
                maxDist = dist;
                first = spot;
            }
        }
        centers.add(first);

        while (centers.size() < k) {
            SpotSnapshot best = null;
            double bestMinDist = -1;

            for (SpotSnapshot spot : spots) {
                if (centers.contains(spot)) {
                    continue;
                }
                double minDist = Double.MAX_VALUE;
                for (SpotSnapshot center : centers) {
                    double dist = calculateDistance(
                            spot.latitude(), spot.longitude(),
                            center.latitude(), center.longitude()
                    );
                    minDist = Math.min(minDist, dist);
                }
                if (minDist > bestMinDist
                        || (minDist == bestMinDist && (best == null || spot.spotId() < best.spotId()))) {
                    bestMinDist = minDist;
                    best = spot;
                }
            }
            centers.add(best);
        }

        return centers;
    }

    /**
     * Regret 기반 Greedy 할당.
     * regret이 큰 스팟부터 먼저 할당하여 특정 클러스터에 꼭 들어가야 하는 스팟의 자리를 확보한다.
     * regret = (2번째로 가까운 center 거리) - (가장 가까운 center 거리)
     */
    private List<List<SpotSnapshot>> assignToClusters(
            List<SpotSnapshot> spots,
            List<SpotSnapshot> centers,
            int[] capacities
    ) {
        int k = centers.size();
        List<List<SpotSnapshot>> clusters = new ArrayList<>();
        int[] remaining = new int[k];
        for (int i = 0; i < k; i++) {
            clusters.add(new ArrayList<>());
            remaining[i] = capacities[i];
        }

        // 중심 스팟을 먼저 해당 클러스터에 할당
        for (int i = 0; i < k; i++) {
            clusters.get(i).add(centers.get(i));
            remaining[i]--;
        }

        // 미할당 스팟 수집
        List<SpotSnapshot> unassigned = new ArrayList<>();
        for (SpotSnapshot spot : spots) {
            if (!centers.contains(spot)) {
                unassigned.add(spot);
            }
        }

        // regret 내림차순 정렬 (동률 시 spotId 오름차순)
        unassigned.sort((a, b) -> {
            double regretA = calculateRegret(a, centers);
            double regretB = calculateRegret(b, centers);
            int cmp = Double.compare(regretB, regretA);
            return cmp != 0 ? cmp : Long.compare(a.spotId(), b.spotId());
        });

        // 자리 있는 가장 가까운 클러스터에 할당
        for (SpotSnapshot spot : unassigned) {
            int bestCluster = -1;
            double bestDist = Double.MAX_VALUE;

            for (int i = 0; i < k; i++) {
                if (remaining[i] <= 0) {
                    continue;
                }
                double dist = calculateDistance(
                        spot.latitude(), spot.longitude(),
                        centers.get(i).latitude(), centers.get(i).longitude()
                );
                if (dist < bestDist || (dist == bestDist && (bestCluster == -1 || i < bestCluster))) {
                    bestDist = dist;
                    bestCluster = i;
                }
            }

            clusters.get(bestCluster).add(spot);
            remaining[bestCluster]--;
        }

        return clusters;
    }

    private double calculateRegret(SpotSnapshot spot, List<SpotSnapshot> centers) {
        if (centers.size() <= 1) {
            return 0;
        }

        double closest = Double.MAX_VALUE;
        double secondClosest = Double.MAX_VALUE;

        for (SpotSnapshot center : centers) {
            double dist = calculateDistance(
                    spot.latitude(), spot.longitude(),
                    center.latitude(), center.longitude()
            );
            if (dist < closest) {
                secondClosest = closest;
                closest = dist;
            } else if (dist < secondClosest) {
                secondClosest = dist;
            }
        }

        return secondClosest - closest;
    }

    /**
     * 클러스터를 중심 경도순으로 정렬한다.
     * 이동 최적화가 아니라 같은 입력에서 Day 매핑 순서를 일정하게 만드는 deterministic policy.
     * 경도 동률 시 위도 오름차순.
     */
    private void sortClustersByLongitude(List<List<SpotSnapshot>> clusters) {
        clusters.sort(Comparator
                .comparingDouble((List<SpotSnapshot> cluster) ->
                        cluster.stream().mapToDouble(SpotSnapshot::longitude).average().orElse(0))
                .thenComparingDouble(cluster ->
                        cluster.stream().mapToDouble(SpotSnapshot::latitude).average().orElse(0))
        );
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

    /**
     * 위도/경도 기반 보정 유클리드 제곱 거리.
     * cos(avgLat) 보정으로 경도 방향 왜곡을 해소한다.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat2 - lat1;
        double avgLat = Math.toRadians((lat1 + lat2) / 2.0);
        double dLon = (lon2 - lon1) * Math.cos(avgLat);
        return dLat * dLat + dLon * dLon;
    }
}
