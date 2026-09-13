package com.moodi.admin.presentation.dto;

import com.moodi.admin.application.dto.AdminAuditLogItem;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminAuditLogResponse(Long id, UUID adminId, String method, String path, int statusCode,
                                    String requestId, LocalDateTime createdAt) {

    public static AdminAuditLogResponse from(AdminAuditLogItem item) {
        return new AdminAuditLogResponse(item.id(), item.adminId(), item.method(), item.path(), item.statusCode(),
                item.requestId(), item.createdAt());
    }
}
