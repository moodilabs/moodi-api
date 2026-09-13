package com.moodi.admin.infrastructure;

import com.moodi.admin.domain.AdminAccountRepository;
import com.moodi.shared.auth.AdminAuthInterceptor;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.UUID;

/**
 * 초기 비밀번호 상태(`passwordChangeRequired`)인 관리자는 비밀번호 변경·내 정보·로그아웃만 허용하고 나머지는 403으로 막는다.
 * 인증 인터셉터가 남긴 adminId가 있을 때만 검사하므로 로그인·재발급은 통과한다.
 */
@Component
public class AdminPasswordChangeInterceptor implements HandlerInterceptor {

    static final Set<String> ALLOWED_PATHS = Set.of("/api/admin/me", "/api/admin/me/password", "/api/admin/auth/logout");

    private final AdminAccountRepository adminAccountRepository;

    public AdminPasswordChangeInterceptor(AdminAccountRepository adminAccountRepository) {
        this.adminAccountRepository = adminAccountRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object adminId = request.getAttribute(AdminAuthInterceptor.ADMIN_ID_ATTRIBUTE);
        if (!(adminId instanceof UUID id) || ALLOWED_PATHS.contains(request.getRequestURI())) {
            return true;
        }
        boolean required = adminAccountRepository.findById(id)
                .map(account -> account.isPasswordChangeRequired())
                .orElse(false);
        if (required) {
            throw new BusinessException(ErrorCode.ADMIN_PASSWORD_CHANGE_REQUIRED);
        }
        return true;
    }
}
