package com.moodi.admin.application;

import com.moodi.admin.application.dto.DashboardSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final DashboardReadModel dashboardReadModel;
    private final Clock clock;

    public DashboardQueryService(DashboardReadModel dashboardReadModel, Clock clock) {
        this.dashboardReadModel = dashboardReadModel;
        this.clock = clock;
    }

    public DashboardSummary getSummary() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay();
        return new DashboardSummary(
                dashboardReadModel.countMembers(todayStart, weekStart),
                dashboardReadModel.countContent(),
                dashboardReadModel.countInquiries(weekStart)
        );
    }
}
