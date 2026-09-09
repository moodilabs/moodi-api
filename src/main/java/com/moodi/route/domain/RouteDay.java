package com.moodi.route.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteDay {

    private Long id;
    private int dayNumber;
    private LocalDate date;
    private List<RouteSpot> spots = new ArrayList<>();
    private List<RouteLeg> legs = new ArrayList<>();

    private RouteDay(int dayNumber, LocalDate date, List<RouteSpot> spots, List<RouteLeg> legs) {
        validateSpotSequences(spots);
        this.dayNumber = dayNumber;
        this.date = date;
        this.spots = spots;
        this.legs = legs;
    }

    public static RouteDay create(int dayNumber, LocalDate date, List<RouteSpot> spots,
                                   List<RouteLeg> legs) {
        return new RouteDay(dayNumber, date, spots, legs);
    }

    public RouteDay copy() {
        List<RouteSpot> copiedSpots = this.spots.stream()
                .map(RouteSpot::copy)
                .toList();
        List<RouteLeg> copiedLegs = this.legs.stream()
                .map(RouteLeg::copy)
                .toList();
        return new RouteDay(this.dayNumber, this.date, copiedSpots, copiedLegs);
    }

    public int getSpotCount() {
        return spots.size();
    }

    public int getNextSequence() {
        return spots.size() + 1;
    }

    public Optional<RouteSpot> getLastSpot() {
        if (spots.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(spots.get(spots.size() - 1));
    }

    /**
     * 영속 상태의 Day를 새 인스턴스로 갈아끼우면 같은 day_number의 행이 삭제 전에 삽입되어 유니크 제약에 걸린다.
     * 그래서 마지막 스팟 추가는 기존 Day를 그대로 두고 컬렉션에 덧붙인다.
     */
    public void addSpot(RouteSpot spot) {
        boolean duplicated = spots.stream()
                .anyMatch(existing -> existing.getSequence() == spot.getSequence());
        if (duplicated) {
            throw new BusinessException(ErrorCode.ROUTE_DUPLICATE_SPOT_SEQUENCE);
        }
        spots.add(spot);
    }

    public void addLeg(RouteLeg leg) {
        legs.add(leg);
    }

    private void validateSpotSequences(List<RouteSpot> spots) {
        Set<Integer> sequences = new HashSet<>();
        for (RouteSpot spot : spots) {
            if (!sequences.add(spot.getSequence())) {
                throw new BusinessException(ErrorCode.ROUTE_DUPLICATE_SPOT_SEQUENCE);
            }
        }
    }
}
