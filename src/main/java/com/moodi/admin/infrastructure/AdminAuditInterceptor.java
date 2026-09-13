package com.moodi.admin.infrastructure;

import com.moodi.admin.application.AdminAuditService;
import com.moodi.shared.auth.AdminAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.UUID;

/**
 * `/api/admin/**`의 변경 요청(POST/PUT/PATCH/DELETE)이 2xx로 끝나면 감사 로그를 남긴다.
 * 인증을 통과한 요청만 대상이므로 로그인·재발급은 남지 않는다(adminId가 없다).
 * 기록 실패는 로그만 남기고 삼킨다 — 감사 기록 때문에 이미 성공한 요청을 실패로 바꾸지 않는다.
 */
@Component
public class AdminAuditInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditInterceptor.class);
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    private final AdminAuditService adminAuditService;

    public AdminAuditInterceptor(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        if (!MUTATING_METHODS.contains(request.getMethod())) {
            return;
        }
        Object adminId = request.getAttribute(AdminAuthInterceptor.ADMIN_ID_ATTRIBUTE);
        if (!(adminId instanceof UUID id) || !HttpStatus.valueOf(response.getStatus()).is2xxSuccessful()) {
            return;
        }
        try {
            adminAuditService.record(id, request.getMethod(), request.getRequestURI(), response.getStatus(),
                    MDC.get(REQUEST_ID_MDC_KEY));
        } catch (RuntimeException e) {
            log.error("감사 로그 기록 실패: {} {} by {}", request.getMethod(), request.getRequestURI(), id, e);
        }
    }
}
