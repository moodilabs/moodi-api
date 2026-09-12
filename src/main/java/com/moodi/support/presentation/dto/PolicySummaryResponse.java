package com.moodi.support.presentation.dto;

import com.moodi.support.application.dto.PolicySummary;
import com.moodi.support.domain.PolicyType;

import java.time.LocalDate;

public record PolicySummaryResponse(PolicyType type, String version, LocalDate effectiveAt) {

    public static PolicySummaryResponse from(PolicySummary summary) {
        return new PolicySummaryResponse(summary.type(), summary.version(), summary.effectiveAt());
    }
}
