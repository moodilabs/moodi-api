package com.moodi.support.application.dto;

import com.moodi.support.domain.Policy;
import com.moodi.support.domain.PolicyType;

import java.time.LocalDate;

/** @param agreed 이 버전에 동의한 회원이 있는지 — 어드민 조회에서만 채우고, 앱 조회는 false */
public record PolicySummary(Long id, PolicyType type, String version, LocalDate effectiveAt, String locale, boolean enabled, boolean visible, boolean agreed) {

    public PolicySummary(Long id, PolicyType type, String version, LocalDate effectiveAt) {
        this(id, type, version, effectiveAt, "en-US", true, true, false);
    }

    public static PolicySummary from(Policy policy) {
        return from(policy, false);
    }

    public static PolicySummary from(Policy policy, boolean agreed) {
        return new PolicySummary(policy.getId(), policy.getType(), policy.getVersion(), policy.getEffectiveAt(), policy.getLocale(), policy.isEnabled(), policy.isVisible(), agreed);
    }
}
