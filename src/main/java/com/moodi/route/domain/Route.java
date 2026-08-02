package com.moodi.route.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Route extends BaseEntity {

    public static final int MAX_DAYS = 5;
    public static final int MAX_TITLE_LENGTH = 40;

    private Long id;
    private UUID publicId;
    private UUID memberId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<RouteDay> days = new ArrayList<>();
    private LocalDateTime deletedAt;

    private Route(UUID memberId, String title, LocalDate startDate, LocalDate endDate,
                  List<RouteDay> days) {
        validateTitle(title);
        validateDateRange(startDate, endDate);
        validateDays(days);
        this.publicId = UUID.randomUUID();
        this.memberId = memberId;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.days = new ArrayList<>(days);
    }

    public static Route create(UUID memberId, String title, LocalDate startDate, LocalDate endDate,
                                List<RouteDay> days) {
        return new Route(memberId, title, startDate, endDate, days);
    }

    public void validateOwner(UUID requestMemberId) {
        if (!this.memberId.equals(requestMemberId)) {
            throw new BusinessException(ErrorCode.ROUTE_FORBIDDEN);
        }
    }

    public void update(String title, LocalDate startDate, LocalDate endDate, List<RouteDay> days) {
        validateTitle(title);
        validateDateRange(startDate, endDate);
        validateDays(days);
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.days.clear();
        this.days.addAll(days);
    }

    public void updateTitle(String title) {
        validateTitle(title);
        this.title = title;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public int getTotalDays() {
        return (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank() || title.strip().length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(ErrorCode.ROUTE_INVALID_TITLE);
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        int totalDays = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
        if (totalDays < 1 || totalDays > MAX_DAYS) {
            throw new BusinessException(ErrorCode.ROUTE_INVALID_DATE_RANGE);
        }
    }

    private void validateDays(List<RouteDay> days) {
        Set<Integer> dayNumbers = new HashSet<>();
        for (RouteDay day : days) {
            if (!dayNumbers.add(day.getDayNumber())) {
                throw new BusinessException(ErrorCode.ROUTE_DUPLICATE_DAY_NUMBER);
            }
        }
    }
}
