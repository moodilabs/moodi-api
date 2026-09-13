package com.moodi.admin.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 변경 요청 기록. 누가·언제·어느 엔드포인트를 성공시켰는지만 남긴다.
 * 요청 본문은 개인정보가 섞일 수 있어 저장하지 않는다 — 상세는 `requestId`로 앱 로그와 대조한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    private Long id;
    private UUID adminId;
    private String method;
    private String path;
    private int statusCode;
    private String requestId;
    private LocalDateTime createdAt;

    private AdminAuditLog(UUID adminId, String method, String path, int statusCode, String requestId,
                          LocalDateTime createdAt) {
        this.adminId = adminId;
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    public static AdminAuditLog record(UUID adminId, String method, String path, int statusCode, String requestId,
                                       LocalDateTime now) {
        return new AdminAuditLog(adminId, method, path, statusCode, requestId, now);
    }
}
