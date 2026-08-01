package com.moodi.route.support;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteDay;
import com.moodi.route.domain.RouteLeg;
import com.moodi.route.domain.RouteSpot;
import com.moodi.route.domain.TravelMode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RouteFixture {

    private static final UUID DEFAULT_MEMBER_ID = UUID.randomUUID();
    private static final String DEFAULT_TITLE = "성수 1박 2일 코스";
    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2026, 8, 10);

    public static Route createRoute() {
        return createRoute(2);
    }

    public static Route createRoute(int totalDays) {
        LocalDate endDate = DEFAULT_START_DATE.plusDays(totalDays - 1);
        List<RouteDay> days = new ArrayList<>();
        for (int i = 1; i <= totalDays; i++) {
            days.add(createDay(i, DEFAULT_START_DATE.plusDays(i - 1), 2));
        }
        return Route.create(DEFAULT_MEMBER_ID, DEFAULT_TITLE, DEFAULT_START_DATE, endDate, days);
    }

    public static Route createRoute(UUID memberId, String title, LocalDate startDate,
                                     LocalDate endDate, List<RouteDay> days) {
        return Route.create(memberId, title, startDate, endDate, days);
    }

    public static RouteDay createDay(int dayNumber, LocalDate date, int spotCount) {
        List<RouteSpot> spots = new ArrayList<>();
        for (int i = 1; i <= spotCount; i++) {
            spots.add(createSpot(i));
        }
        List<RouteLeg> legs = new ArrayList<>();
        for (int i = 1; i < spotCount; i++) {
            legs.add(createLeg(i, i + 1));
        }
        return RouteDay.create(dayNumber, date, spots, legs);
    }

    public static RouteSpot createSpot(int sequence) {
        return RouteSpot.create(
                (long) sequence, sequence, 60,
                "스팟 " + sequence, "https://img.example.com/" + sequence + ".jpg",
                "서울", "성동구",
                37.5445 + sequence * 0.001, 127.0560 + sequence * 0.001,
                "TOURIST_ATTRACTION", null
        );
    }

    public static RouteLeg createLeg(int fromSequence, int toSequence) {
        return RouteLeg.create(fromSequence, toSequence, TravelMode.WALK, 600, 800, null);
    }

    private RouteFixture() {
    }
}
