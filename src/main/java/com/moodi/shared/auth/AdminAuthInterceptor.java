package com.moodi.shared.auth;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * `/api/admin/**` 전용. {@link AdminRequired}가 붙은 핸들러만 검사하고, 로그인·재발급처럼 붙지 않은 것은 통과시킨다.
 * 회원 토큰은 `type` 클레임이 달라 파싱 단계에서 걸러진다.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ADMIN_ID_ATTRIBUTE = "authAdminId";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public AdminAuthInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        AdminRequired required = findAnnotation(handlerMethod);
        if (required == null) {
            return true;
        }
        AdminPrincipal principal = jwtProvider.parseAdminAccessToken(extractToken(request))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!principal.role().covers(required.role())) {
            throw new BusinessException(ErrorCode.ADMIN_FORBIDDEN);
        }
        request.setAttribute(ADMIN_ID_ATTRIBUTE, principal.adminId());
        return true;
    }

    /** 메서드 애노테이션이 클래스 애노테이션보다 우선한다 (메서드 단위로 SUPER를 요구할 수 있게). */
    private AdminRequired findAnnotation(HandlerMethod handlerMethod) {
        AdminRequired onMethod = handlerMethod.getMethodAnnotation(AdminRequired.class);
        if (onMethod != null) {
            return onMethod;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), AdminRequired.class);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
