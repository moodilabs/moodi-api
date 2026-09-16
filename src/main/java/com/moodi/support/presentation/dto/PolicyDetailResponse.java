package com.moodi.support.presentation.dto;

import com.moodi.support.application.dto.PolicyDetail;
import com.moodi.support.domain.PolicyType;

import java.time.LocalDate;

public record PolicyDetailResponse(PolicyType type, String version, String content, LocalDate effectiveAt, Long id, String locale) {

    public static PolicyDetailResponse from(PolicyDetail detail) {
        return new PolicyDetailResponse(detail.type(), detail.version(), detail.content(), detail.effectiveAt(), detail.id(), detail.locale());
    }
}
