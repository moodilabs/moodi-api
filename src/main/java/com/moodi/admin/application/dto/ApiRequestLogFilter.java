package com.moodi.admin.application.dto;

import java.util.UUID;

/**
 * @param method 정확 일치 (GET/POST/…). null이면 전체
 * @param path   부분 일치. null이면 전체
 * @param statusClass 2·4·5 같은 백의 자리. null이면 전체
 */
public record ApiRequestLogFilter(UUID memberId, String method, String path, Integer statusClass) {
}
