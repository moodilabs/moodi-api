package com.moodi.support.application.dto;

import com.moodi.support.domain.Policy;
import com.moodi.support.domain.PolicyType;

import java.time.LocalDate;

public record PolicyDetail(Long id, PolicyType type, String version, String content, LocalDate effectiveAt, String locale, boolean enabled, boolean visible) {

    public PolicyDetail(Long id, PolicyType type, String version, String content, LocalDate effectiveAt) {
        this(id, type, version, content, effectiveAt, "en-US", true, true);
    }

    public static PolicyDetail from(Policy policy) {
        return new PolicyDetail(policy.getId(), policy.getType(), policy.getVersion(), policy.getContent(),
                policy.getEffectiveAt(), policy.getLocale(), policy.isEnabled(), policy.isVisible());
    }
}
