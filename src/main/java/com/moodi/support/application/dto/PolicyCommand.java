package com.moodi.support.application.dto;

import com.moodi.support.domain.PolicyType;

import java.time.LocalDate;

public record PolicyCommand(PolicyType type, String version, String content, LocalDate effectiveAt) {
}
