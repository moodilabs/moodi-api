package com.moodi.support.presentation.dto.admin;

import com.moodi.support.application.dto.PolicySummary;
import com.moodi.support.domain.PolicyType;

import java.time.LocalDate;

public record AdminPolicySummaryResponse(Long id, PolicyType type, String version, LocalDate effectiveAt, String locale, boolean enabled, boolean visible, boolean agreed) {

    public static AdminPolicySummaryResponse from(PolicySummary summary) {
        return new AdminPolicySummaryResponse(summary.id(), summary.type(), summary.version(), summary.effectiveAt(), summary.locale(), summary.enabled(), summary.visible(), summary.agreed());
    }
}
