package com.moodi.member.application.dto;

import com.moodi.member.domain.AgreementType;

import java.util.Map;

public record AgreementCommand(Map<AgreementType, Boolean> values, String locale, Map<AgreementType, Long> policyIds) {
    public AgreementCommand(Map<AgreementType, Boolean> values) { this(values, "en-US", Map.of()); }
    public AgreementCommand {
        values = Map.copyOf(values);
        locale = locale == null ? "en-US" : locale;
        if (!"en-US".equals(locale) && !"ko-KR".equals(locale)) throw new com.moodi.shared.error.BusinessException(com.moodi.shared.error.ErrorCode.INVALID_REQUEST);
        policyIds = policyIds == null ? Map.of() : Map.copyOf(policyIds);
    }


    public static AgreementCommand of(Map<AgreementType, Boolean> values) {
        return new AgreementCommand(Map.copyOf(values));
    }

    public boolean isAgreed(AgreementType type) {
        return Boolean.TRUE.equals(values.get(type));
    }
}
