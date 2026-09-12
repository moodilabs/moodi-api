package com.moodi.support.support;

import com.moodi.support.domain.Policy;
import com.moodi.support.domain.PolicyType;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

public class PolicyFixture {

    public static final String DEFAULT_CONTENT = "Article 1 (Purpose and Definitions)\n1. These Terms of Service ...";

    public static Policy create(PolicyType type, String version, LocalDate effectiveAt) {
        return Policy.create(type, version, DEFAULT_CONTENT, effectiveAt);
    }

    public static Policy createWithId(Long id, PolicyType type, String version, LocalDate effectiveAt) {
        Policy policy = create(type, version, effectiveAt);
        ReflectionTestUtils.setField(policy, "id", id);
        return policy;
    }

    private PolicyFixture() {
    }
}
