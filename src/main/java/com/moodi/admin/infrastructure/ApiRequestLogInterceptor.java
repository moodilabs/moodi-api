package com.moodi.admin.infrastructure;

import com.moodi.admin.application.ApiRequestLogService;
import com.moodi.shared.auth.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 앱 API(`/api/**`, 관리자 제외) 요청을 결과와 무관하게 한 줄씩 남긴다.
 * 인증 인터셉터보다 먼저(order 음수) 걸어야 401로 끝난 요청도 afterCompletion에 도달한다 — 회원 ID는 인증 인터셉터가
 * 남긴 속성을 완료 시점에 읽으므로 순서와 무관하게 채워진다.
 * 기록 실패는 로그만 남기고 삼킨다.
 */
@Component
public class ApiRequestLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestLogInterceptor.class);
    private static final String START_ATTRIBUTE = ApiRequestLogInterceptor.class.getName() + ".start";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    private final ApiRequestLogService apiRequestLogService;

    public ApiRequestLogInterceptor(ApiRequestLogService apiRequestLogService) {
        this.apiRequestLogService = apiRequestLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTRIBUTE, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        if ("OPTIONS".equals(request.getMethod())) {
            return;
        }
        Object memberId = request.getAttribute(AuthInterceptor.MEMBER_ID_ATTRIBUTE);
        Object start = request.getAttribute(START_ATTRIBUTE);
        int durationMs = start instanceof Long s ? (int) ((System.nanoTime() - s) / 1_000_000) : 0;
        try {
            apiRequestLogService.record(memberId instanceof UUID id ? id : null, request.getMethod(),
                    request.getRequestURI(), response.getStatus(), durationMs, MDC.get(REQUEST_ID_MDC_KEY));
        } catch (RuntimeException e) {
            log.error("API 요청 로그 기록 실패: {} {}", request.getMethod(), request.getRequestURI(), e);
        }
    }
}
