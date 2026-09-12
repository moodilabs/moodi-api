package com.moodi.support.application.dto;

import com.moodi.support.domain.Policy;
import com.moodi.support.domain.PolicyType;

import java.time.LocalDate;

public record PolicyDetail(Long id, PolicyType type, String version, String content, LocalDate effectiveAt) {

    public static PolicyDetail from(Policy policy) {
        return new PolicyDetail(policy.getId(), policy.getType(), policy.getVersion(), policy.getContent(),
                policy.getEffectiveAt());
    }
}
