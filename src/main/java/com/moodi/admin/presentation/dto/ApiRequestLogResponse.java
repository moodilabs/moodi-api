package com.moodi.admin.presentation.dto;

import com.moodi.admin.application.dto.ApiRequestLogItem;

import java.time.LocalDateTime;
import java.util.UUID;

public record ApiRequestLogResponse(Long id, UUID memberId, String memberNickname, String memberEmail, String method,
                                    String path, int statusCode, int durationMs, String requestId,
                                    LocalDateTime createdAt) {

    public static ApiRequestLogResponse from(ApiRequestLogItem item) {
        return new ApiRequestLogResponse(item.id(), item.memberId(), item.memberNickname(), item.memberEmail(),
                item.method(), item.path(), item.statusCode(), item.durationMs(), item.requestId(),
                item.createdAt());
    }
}
