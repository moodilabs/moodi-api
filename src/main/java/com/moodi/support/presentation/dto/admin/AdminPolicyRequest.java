package com.moodi.support.presentation.dto.admin;

import com.moodi.support.application.dto.PolicyCommand;
import com.moodi.support.domain.PolicyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AdminPolicyRequest(
        @NotNull(message = "종류는 필수입니다.")
        PolicyType type,

        @NotBlank(message = "버전은 필수입니다.")
        @Size(max = 20, message = "버전은 20자 이하여야 합니다.")
        String version,

        @NotBlank(message = "전문은 필수입니다.")
        String content,

        @NotNull(message = "시행일은 필수입니다.")
        LocalDate effectiveAt,
        @jakarta.validation.constraints.Pattern(regexp = "ko-KR|en-US") String locale,
        Boolean enabled,
        Boolean visible
) {

    public AdminPolicyRequest(PolicyType type, String version, String content, LocalDate effectiveAt) {
        this(type, version, content, effectiveAt, "en-US", true, true);
    }

    public PolicyCommand toCommand() {
        return new PolicyCommand(type, version, content, effectiveAt, locale == null ? "en-US" : locale, enabled == null || enabled, visible == null || visible);
    }
}
