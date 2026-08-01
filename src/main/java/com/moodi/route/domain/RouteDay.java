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
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteDay {

    public static final int MAX_SPOTS_PER_DAY = 6;

    private Long id;
    private int dayNumber;
    private LocalDate date;
    private List<RouteSpot> spots = new ArrayList<>();
    private List<RouteLeg> legs = new ArrayList<>();

    private RouteDay(int dayNumber, LocalDate date, List<RouteSpot> spots, List<RouteLeg> legs) {
        validateSpotCount(spots);
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

    public int getSpotCount() {
        return spots.size();
    }

    private void validateSpotCount(List<RouteSpot> spots) {
        if (spots.size() > MAX_SPOTS_PER_DAY) {
            throw new BusinessException(ErrorCode.ROUTE_DAY_SPOT_LIMIT_EXCEEDED);
        }
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
