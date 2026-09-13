package com.moodi.admin.infrastructure;

import com.moodi.admin.application.AdminAuditService;
import com.moodi.shared.auth.AdminAuthInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAuditInterceptorTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private AdminAuditInterceptor interceptor;

    @Test
    @DisplayName("인증된 관리자의 변경 요청이 2xx면 기록한다")
    void records_successful_mutation() {
        MockHttpServletRequest request = request("PATCH", "/api/admin/members/x/status", ADMIN_ID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(204);

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(adminAuditService).record(eq(ADMIN_ID), eq("PATCH"), eq("/api/admin/members/x/status"), eq(204),
                isNull());
    }

    @Test
    @DisplayName("조회 요청은 기록하지 않는다")
    void skips_get() {
        MockHttpServletRequest request = request("GET", "/api/admin/members", ADMIN_ID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(adminAuditService, never()).record(any(), anyString(), anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("실패한 요청은 기록하지 않는다")
    void skips_failed_request() {
        MockHttpServletRequest request = request("POST", "/api/admin/notices", ADMIN_ID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(400);

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(adminAuditService, never()).record(any(), anyString(), anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("인증되지 않은 요청(로그인 등)은 기록하지 않는다")
    void skips_unauthenticated_request() {
        MockHttpServletRequest request = request("POST", "/api/admin/auth/login", null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(adminAuditService, never()).record(any(), anyString(), anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("기록이 실패해도 예외를 던지지 않는다")
    void swallows_record_failure() {
        MockHttpServletRequest request = request("DELETE", "/api/admin/notices/1", ADMIN_ID);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(204);
        doThrow(new RuntimeException("db down")).when(adminAuditService)
                .record(any(), anyString(), anyString(), anyInt(), any());

        interceptor.afterCompletion(request, response, new Object(), null);
    }

    private MockHttpServletRequest request(String method, String uri, UUID adminId) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (adminId != null) {
            request.setAttribute(AdminAuthInterceptor.ADMIN_ID_ATTRIBUTE, adminId);
        }
        return request;
    }
}
