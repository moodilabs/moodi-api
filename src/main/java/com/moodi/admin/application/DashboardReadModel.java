package com.moodi.admin.application;

import com.moodi.admin.application.dto.DashboardSummary;

import java.time.LocalDateTime;

/**
 * 대시보드 집계 포트. 여러 컨텍스트 테이블을 읽기 전용으로 센다 — 어떤 컨텍스트에도 쓰지 않으므로
 * 경계 규칙(ID만 참조)을 어기지 않는다.
 *
 * @param todayStart 오늘 0시 (newToday 기준)
 * @param weekStart  7일 전 0시 (newLast7Days·answeredLast7Days 기준)
 */
public interface DashboardReadModel {

    DashboardSummary.Members countMembers(LocalDateTime todayStart, LocalDateTime weekStart);

    DashboardSummary.Content countContent();

    DashboardSummary.Inquiries countInquiries(LocalDateTime weekStart);
}
