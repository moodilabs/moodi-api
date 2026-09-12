package com.moodi.support.presentation.dto.admin;

import com.moodi.support.application.dto.PolicyDetail;
import com.moodi.support.domain.PolicyType;

import java.time.LocalDate;

public record AdminPolicyDetailResponse(Long id, PolicyType type, String version, String content,
                                        LocalDate effectiveAt) {

    public static AdminPolicyDetailResponse from(PolicyDetail detail) {
        return new AdminPolicyDetailResponse(detail.id(), detail.type(), detail.version(), detail.content(),
                detail.effectiveAt());
    }
}
