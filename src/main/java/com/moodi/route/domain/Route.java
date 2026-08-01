package com.moodi.route.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Route extends BaseEntity {

    private Long id;
    private UUID publicId;
    private UUID memberId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<RouteDay> days = new ArrayList<>();

    private Route(UUID memberId, String title, LocalDate startDate, LocalDate endDate,
                  List<RouteDay> days) {
        this.publicId = UUID.randomUUID();
        this.memberId = memberId;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.days = days;
    }

    public static Route create(UUID memberId, String title, LocalDate startDate, LocalDate endDate,
                                List<RouteDay> days) {
        return new Route(memberId, title, startDate, endDate, days);
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public int getTotalDays() {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }
}
