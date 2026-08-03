package com.moodi.member.presentation.dto;

import com.moodi.member.application.dto.AgreementCommand;
import com.moodi.member.domain.AgreementType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AgreementRequest(
        @NotNull(message = "이용약관 동의 여부는 필수입니다.")
        Boolean termsOfService,

        @NotNull(message = "개인정보 수집·이용 동의 여부는 필수입니다.")
        Boolean privacyPolicy,

        @NotNull(message = "만 14세 이상 확인 여부는 필수입니다.")
        Boolean ageOver14,

        @NotNull(message = "마케팅 수신 동의 여부는 필수입니다.")
        Boolean marketing
) {

    public AgreementCommand toCommand() {
        return AgreementCommand.of(Map.of(
                AgreementType.TERMS_OF_SERVICE, termsOfService,
                AgreementType.PRIVACY_POLICY, privacyPolicy,
                AgreementType.AGE_OVER_14, ageOver14,
                AgreementType.MARKETING, marketing
        ));
    }
}
