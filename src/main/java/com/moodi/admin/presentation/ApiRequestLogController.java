package com.moodi.admin.presentation;

import com.moodi.admin.application.ApiRequestLogService;
import com.moodi.admin.application.dto.ApiRequestLogFilter;
import com.moodi.admin.presentation.dto.ApiRequestLogResponse;
import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 앱 API 요청 로그 열람. 회원 식별 정보가 붙으므로 SUPER만. */
@AdminRequired(role = AdminRole.SUPER)
@RestController
public class ApiRequestLogController {

    private final ApiRequestLogService apiRequestLogService;

    public ApiRequestLogController(ApiRequestLogService apiRequestLogService) {
        this.apiRequestLogService = apiRequestLogService;
    }

    /**
     * @param statusClass 2·4·5 — 응답 코드 백의 자리
     */
    @GetMapping("/api/admin/api-logs")
    public SuccessResponse<CursorResponse<ApiRequestLogResponse>> getLogs(
            @RequestParam(required = false) UUID memberId,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) Integer statusClass,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int size
    ) {
        if (statusClass != null && (statusClass < 1 || statusClass > 5)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        ApiRequestLogFilter filter = new ApiRequestLogFilter(memberId, method, path, statusClass);
        return SuccessResponse.of(apiRequestLogService.getLogs(filter, parseCursor(cursor), size)
                .map(ApiRequestLogResponse::from));
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
