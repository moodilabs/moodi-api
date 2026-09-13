package com.moodi.admin.application.dto;

import com.moodi.admin.domain.AdminAuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminAuditLogItem(Long id, UUID adminId, String method, String path, int statusCode, String requestId,
                                LocalDateTime createdAt) {

    public static AdminAuditLogItem from(AdminAuditLog log) {
        return new AdminAuditLogItem(log.getId(), log.getAdminId(), log.getMethod(), log.getPath(),
                log.getStatusCode(), log.getRequestId(), log.getCreatedAt());
    }
}
