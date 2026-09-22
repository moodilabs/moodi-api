package com.moodi.admin.presentation;

import com.moodi.admin.application.AdminAuditService;
import com.moodi.admin.presentation.dto.AdminAuditLogResponse;
import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 감사 로그 열람. 모든 관리자. */
@AdminRequired
@RestController
public class AdminAuditLogController {

    private final AdminAuditService adminAuditService;

    public AdminAuditLogController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping("/api/admin/audit-logs")
    public SuccessResponse<CursorResponse<AdminAuditLogResponse>> getLogs(
            @RequestParam(required = false) UUID adminId,
            @RequestParam(required = false) String cursor
    ) {
        return SuccessResponse.of(adminAuditService.getLogs(adminId, parseCursor(cursor))
                .map(AdminAuditLogResponse::from));
    }

    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR_FORMAT);
        }
    }
}
