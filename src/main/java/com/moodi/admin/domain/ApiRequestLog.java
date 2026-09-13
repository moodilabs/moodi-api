package com.moodi.admin.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 앱 API 요청 기록. 어느 회원(또는 비회원)이 언제 어느 엔드포인트를 어떤 결과로 호출했는지만 남긴다.
 * 요청 본문·헤더는 개인정보가 섞일 수 있어 저장하지 않는다 — 상세는 `requestId`로 앱 로그와 대조한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiRequestLog {

    public static final int MAX_PATH_LENGTH = 300;

    private Long id;
    private UUID memberId;
    private String method;
    private String path;
    private int statusCode;
    private int durationMs;
    private String requestId;
    private LocalDateTime createdAt;

    private ApiRequestLog(UUID memberId, String method, String path, int statusCode, int durationMs,
                          String requestId, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.method = method;
        this.path = path.length() > MAX_PATH_LENGTH ? path.substring(0, MAX_PATH_LENGTH) : path;
        this.statusCode = statusCode;
        this.durationMs = Math.max(durationMs, 0);
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    public static ApiRequestLog record(UUID memberId, String method, String path, int statusCode, int durationMs,
                                       String requestId, LocalDateTime now) {
        return new ApiRequestLog(memberId, method, path, statusCode, durationMs, requestId, now);
    }
}
