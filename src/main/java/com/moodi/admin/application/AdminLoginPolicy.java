package com.moodi.admin.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 로그인 실패 잠금 정책 (`admin.login.*`). */
@ConfigurationProperties(prefix = "admin.login")
public record AdminLoginPolicy(int maxFailures, int lockMinutes) {
}
