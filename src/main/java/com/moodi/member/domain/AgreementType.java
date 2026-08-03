package com.moodi.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum AgreementType {

    TERMS_OF_SERVICE(true),
    PRIVACY_POLICY(true),
    AGE_OVER_14(true),
    MARKETING(false);

    private static final Set<AgreementType> REQUIRED_TYPES = Stream.of(values())
            .filter(AgreementType::isRequired)
            .collect(Collectors.toUnmodifiableSet());

    private final boolean required;

    public static Set<AgreementType> requiredTypes() {
        return REQUIRED_TYPES;
    }
}
