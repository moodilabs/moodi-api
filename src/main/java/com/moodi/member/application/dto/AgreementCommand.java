package com.moodi.member.application.dto;

import com.moodi.member.domain.AgreementType;

import java.util.Map;

public record AgreementCommand(Map<AgreementType, Boolean> values) {

    public static AgreementCommand of(Map<AgreementType, Boolean> values) {
        return new AgreementCommand(Map.copyOf(values));
    }

    public boolean isAgreed(AgreementType type) {
        return Boolean.TRUE.equals(values.get(type));
    }
}
