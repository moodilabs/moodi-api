package com.moodi.support.application.dto;

import com.moodi.support.domain.Policy;
import com.moodi.support.domain.PolicyType;

import java.time.LocalDate;

public record PolicySummary(Long id, PolicyType type, String version, LocalDate effectiveAt) {

    public static PolicySummary from(Policy policy) {
        return new PolicySummary(policy.getId(), policy.getType(), policy.getVersion(), policy.getEffectiveAt());
    }
}
