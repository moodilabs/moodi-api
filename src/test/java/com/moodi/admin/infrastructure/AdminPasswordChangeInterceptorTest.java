package com.moodi.admin.infrastructure;

import com.moodi.admin.domain.AdminAccount;
import com.moodi.admin.domain.AdminAccountRepository;
import com.moodi.admin.support.AdminAccountFixture;
import com.moodi.shared.auth.AdminAuthInterceptor;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPasswordChangeInterceptorTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Mock
    private AdminAccountRepository adminAccountRepository;

    @InjectMocks
    private AdminPasswordChangeInterceptor interceptor;

    @Test
    @DisplayName("초기 비밀번호 상태면 일반 관리자 API를 403으로 막는다")
    void blocks_when_password_change_required() {
        AdminAccount account = AdminAccountFixture.createWithId(ADMIN_ID, AdminRole.OPERATOR);
        when(adminAccountRepository.findById(ADMIN_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> interceptor.preHandle(authenticated("/api/admin/members"), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_PASSWORD_CHANGE_REQUIRED);
    }

    @Test
    @DisplayName("초기 비밀번호 상태라도 비밀번호 변경·내 정보·로그아웃은 허용한다")
    void allows_password_change_paths() {
        AdminAccount account = AdminAccountFixture.createWithId(ADMIN_ID, AdminRole.OPERATOR);

        for (String path : AdminPasswordChangeInterceptor.ALLOWED_PATHS) {
            assertThat(interceptor.preHandle(authenticated(path), new MockHttpServletResponse(), new Object()))
                    .as(path).isTrue();
        }
        verify(adminAccountRepository, never()).findById(any());
        assertThat(account.isPasswordChangeRequired()).isTrue();
    }

    @Test
    @DisplayName("비밀번호를 바꾼 계정은 통과한다")
    void passes_after_password_changed() {
        AdminAccount account = AdminAccountFixture.createWithId(ADMIN_ID, AdminRole.OPERATOR);
        account.changePassword("new-hash");
        when(adminAccountRepository.findById(ADMIN_ID)).thenReturn(Optional.of(account));

        assertThat(interceptor.preHandle(authenticated("/api/admin/members"), new MockHttpServletResponse(), new Object()))
                .isTrue();
    }

    @Test
    @DisplayName("인증되지 않은 요청(로그인·재발급)은 검사하지 않는다")
    void skips_unauthenticated_requests() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/auth/login");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        verify(adminAccountRepository, never()).findById(any());
    }

    private static MockHttpServletRequest authenticated(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setAttribute(AdminAuthInterceptor.ADMIN_ID_ATTRIBUTE, ADMIN_ID);
        return request;
    }
}
