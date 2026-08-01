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
        validateTitle(title);
        validateDates(startDate, endDate);
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
        validateTitle(title);
        this.title = title;
    }

    public int getTotalDays() {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("루트 제목은 비어있을 수 없습니다");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 40) {
            throw new IllegalArgumentException("루트 제목은 40자를 초과할 수 없습니다");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("여행 시작일과 종료일은 필수입니다");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일 이전이어야 합니다");
        }
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        if (days > 5) {
            throw new IllegalArgumentException("여행 기간은 최대 5일입니다");
        }
    }
}
