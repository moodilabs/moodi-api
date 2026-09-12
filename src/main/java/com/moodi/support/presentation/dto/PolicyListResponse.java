package com.moodi.support.presentation.dto;

import com.moodi.support.application.dto.PolicySummary;

import java.util.List;

public record PolicyListResponse(List<PolicySummaryResponse> policies) {

    public static PolicyListResponse from(List<PolicySummary> summaries) {
        return new PolicyListResponse(summaries.stream().map(PolicySummaryResponse::from).toList());
    }
}
