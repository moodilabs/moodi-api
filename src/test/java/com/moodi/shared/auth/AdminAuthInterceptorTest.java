package com.moodi.shared.auth;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAuthInterceptorTest {

    private static final String SECRET = "test-secret-key-for-jwt-hs256-must-be-at-least-32-bytes";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 1_800_000, 1_209_600_000, 1_800_000, 43_200_000);
    private final AdminAuthInterceptor interceptor = new AdminAuthInterceptor(jwtProvider);

    @Test
    @DisplayName("관리자 토큰이면 통과하고 adminId를 요청에 싣는다")
    void passes_with_admin_token() throws Exception {
        UUID adminId = UUID.randomUUID();
        MockHttpServletRequest request = bearer(jwtProvider.issueAdminAccessToken(adminId, AdminRole.OPERATOR));

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), handler("operatorOnly"));

        assertThat(result).isTrue();
        assertThat(request.getAttribute(AdminAuthInterceptor.ADMIN_ID_ATTRIBUTE)).isEqualTo(adminId);
    }

    @Test
    @DisplayName("회원 토큰으로 관리자 엔드포인트를 호출하면 401이다")
    void rejects_member_token() {
        MockHttpServletRequest request = bearer(jwtProvider.issueAccessToken(UUID.randomUUID()));

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler("operatorOnly")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("토큰이 없으면 401이다")
    void rejects_missing_token() {
        assertThatThrownBy(() -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(),
                handler("operatorOnly")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("OPERATOR가 SUPER 전용 엔드포인트를 호출하면 403이다")
    void rejects_insufficient_role() {
        MockHttpServletRequest request = bearer(jwtProvider.issueAdminAccessToken(UUID.randomUUID(), AdminRole.OPERATOR));

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler("superOnly")))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_FORBIDDEN);
    }

    @Test
    @DisplayName("SUPER는 모든 관리자 엔드포인트를 호출할 수 있다")
    void super_covers_operator_endpoints() throws Exception {
        MockHttpServletRequest request = bearer(jwtProvider.issueAdminAccessToken(UUID.randomUUID(), AdminRole.SUPER));

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler("operatorOnly"))).isTrue();
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler("superOnly"))).isTrue();
    }

    @Test
    @DisplayName("애노테이션이 없는 핸들러(로그인 등)는 토큰 없이 통과한다")
    void passes_unannotated_handler() throws Exception {
        HandlerMethod open = new HandlerMethod(new OpenController(), OpenController.class.getMethod("open"));

        assertThat(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), open)).isTrue();
    }

    private MockHttpServletRequest bearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        Method method = SampleController.class.getMethod(methodName);
        return new HandlerMethod(new SampleController(), method);
    }

    @AdminRequired
    public static class SampleController {

        public void operatorOnly() {
        }

        @AdminRequired(role = AdminRole.SUPER)
        public void superOnly() {
        }
    }

    public static class OpenController {
        public void open() {
        }
    }
}
