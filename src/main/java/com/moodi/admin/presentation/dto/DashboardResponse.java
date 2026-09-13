package com.moodi.admin.presentation.dto;

import com.moodi.admin.application.dto.DashboardSummary;

public record DashboardResponse(DashboardSummary.Members members, DashboardSummary.Content content,
                                DashboardSummary.Inquiries inquiries) {

    public static DashboardResponse from(DashboardSummary summary) {
        return new DashboardResponse(summary.members(), summary.content(), summary.inquiries());
    }
}
