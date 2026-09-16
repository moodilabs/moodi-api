package com.moodi.member.application;

import com.moodi.member.domain.AgreementType;
public interface AgreementPolicyReader {
    record Version(Long id, String version, String locale) {}
    Version findCurrent(AgreementType type, String locale);
}
