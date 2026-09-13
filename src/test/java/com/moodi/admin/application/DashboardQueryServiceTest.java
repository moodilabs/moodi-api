package com.moodi.admin.application;

import com.moodi.admin.application.dto.DashboardSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardQueryServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-10T05:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private DashboardReadModel dashboardReadModel;

    @Test
    @DisplayName("오늘 0시와 7일 전 0시를 기준으로 집계를 모은다")
    void get_summary_uses_today_and_week_boundaries() {
        LocalDateTime todayStart = LocalDateTime.of(2026, 8, 10, 0, 0);
        LocalDateTime weekStart = LocalDateTime.of(2026, 8, 4, 0, 0);
        DashboardSummary.Members members = new DashboardSummary.Members(100, 90, 5, 2, 3, 4, 20);
        DashboardSummary.Content content = new DashboardSummary.Content(3400, 9800, 1200, 210, 560);
        DashboardSummary.Inquiries inquiries = new DashboardSummary.Inquiries(4, 11);
        when(dashboardReadModel.countMembers(todayStart, weekStart)).thenReturn(members);
        when(dashboardReadModel.countContent()).thenReturn(content);
        when(dashboardReadModel.countInquiries(weekStart)).thenReturn(inquiries);

        DashboardSummary summary = new DashboardQueryService(dashboardReadModel, FIXED_CLOCK).getSummary();

        assertThat(summary.members()).isEqualTo(members);
        assertThat(summary.content()).isEqualTo(content);
        assertThat(summary.inquiries()).isEqualTo(inquiries);
        verify(dashboardReadModel).countMembers(todayStart, weekStart);
    }
}
