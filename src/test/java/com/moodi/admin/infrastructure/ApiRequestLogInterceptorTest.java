package com.moodi.admin.infrastructure;

import com.moodi.admin.application.ApiRequestLogService;
import com.moodi.shared.auth.AuthInterceptor;
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
class ApiRequestLogInterceptorTest {

    @Mock
    private ApiRequestLogService apiRequestLogService;

    @InjectMocks
    private ApiRequestLogInterceptor interceptor;

    @Test
    @DisplayName("회원 요청은 회원 ID·메서드·경로·상태로 기록된다")
    void records_member_request() {
        UUID memberId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/feed");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.preHandle(request, response, new Object());
        request.setAttribute(AuthInterceptor.MEMBER_ID_ATTRIBUTE, memberId);
        interceptor.afterCompletion(request, response, new Object(), null);

        verify(apiRequestLogService).record(eq(memberId), eq("GET"), eq("/api/v1/feed"), eq(200), anyInt(), isNull());
    }

    @Test
    @DisplayName("비회원·인증 실패 요청도 회원 ID 없이 기록된다")
    void records_anonymous_request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/routes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(401);

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        verify(apiRequestLogService).record(isNull(), eq("POST"), eq("/api/v1/routes"), eq(401), anyInt(), isNull());
    }

    @Test
    @DisplayName("CORS preflight(OPTIONS)는 기록하지 않는다")
    void skips_preflight() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/feed");

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

        verify(apiRequestLogService, never()).record(any(), anyString(), anyString(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("기록에 실패해도 예외를 밖으로 던지지 않는다")
    void swallows_record_failure() {
        doThrow(new RuntimeException("db down"))
                .when(apiRequestLogService).record(any(), anyString(), anyString(), anyInt(), anyInt(), any());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/feed");

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
    }
}
