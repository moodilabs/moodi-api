package com.moodi.route.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteDay {

    private Long id;
    private int dayNumber;
    private LocalDate date;
    private List<RouteSpot> spots = new ArrayList<>();
    private List<RouteLeg> legs = new ArrayList<>();

    private RouteDay(int dayNumber, LocalDate date, List<RouteSpot> spots, List<RouteLeg> legs) {
        validateDayNumber(dayNumber);
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

    private void validateDayNumber(int dayNumber) {
        if (dayNumber < 1 || dayNumber > 5) {
            throw new IllegalArgumentException("일차는 1~5 범위여야 합니다");
        }
    }
}
