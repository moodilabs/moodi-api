package com.moodi.admin.application;

import com.moodi.admin.application.dto.AdminLoginResult;
import com.moodi.admin.application.dto.AdminTokenPair;
import com.moodi.admin.domain.AdminAccount;
import com.moodi.admin.domain.AdminAccountRepository;
import com.moodi.admin.domain.AdminAccountStatus;
import com.moodi.admin.domain.AdminRefreshToken;
import com.moodi.admin.domain.AdminRefreshTokenRepository;
import com.moodi.admin.support.AdminAccountFixture;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-10T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final AdminTokenPair TOKENS = new AdminTokenPair("access", "refresh", NOW.plusHours(12));

    @Mock
    private AdminAccountRepository adminAccountRepository;

    @Mock
    private AdminRefreshTokenRepository adminRefreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminTokenProvider adminTokenProvider;

    private AdminAuthService adminAuthService;

    @BeforeEach
    void setUp() {
        adminAuthService = new AdminAuthService(adminAccountRepository, adminRefreshTokenRepository,
                passwordEncoder, adminTokenProvider, new AdminLoginPolicy(5, 15), FIXED_CLOCK);
    }

    @Test
    @DisplayName("로그인 성공 시 토큰과 권한을 돌려주고 리프레시 토큰을 저장한다")
    void login_success_issues_tokens() {
        AdminAccount account = AdminAccountFixture.createWithId(ADMIN_ID, AdminRole.OPERATOR);
        when(adminAccountRepository.findByLoginId("ops01")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("correct-password", account.getPasswordHash())).thenReturn(true);
        when(adminTokenProvider.issue(ADMIN_ID, AdminRole.OPERATOR)).thenReturn(TOKENS);

        AdminLoginResult result = adminAuthService.login(" OPS01 ", "correct-password");

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.role()).isEqualTo(AdminRole.OPERATOR);
        assertThat(result.passwordChangeRequired()).isTrue();
        assertThat(account.getLastLoginAt()).isEqualTo(NOW);
        verify(adminRefreshTokenRepository).save(any(AdminRefreshToken.class));
    }

    @Test
    @DisplayName("없는 이메일과 틀린 비밀번호는 같은 에러를 돌려준다")
    void login_failure_does_not_reveal_account_existence() {
        AdminAccount account = AdminAccountFixture.createWithId(ADMIN_ID, AdminRole.OPERATOR);
        when(adminAccountRepository.findByLoginId("ops01")).thenReturn(Optional.of(account));
        when(adminAccountRepository.findByLoginId("nobody")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("wrong", account.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> adminAuthService.login("ops01", "wrong"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_LOGIN_FAILED);
        assertThatThrownBy(() -> adminAuthService.login("nobody", "wrong"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_LOGIN_FAILED);
        assertThat(account.getFailedLoginCount()).isEqualTo(1);
        verify(adminTokenProvider, never()).issue(any(), any());
    }

    @Test
    @DisplayName("잠긴 계정은 비밀번호가 맞아도 로그인할 수 없다")
    void login_locked_account_throws() {
        AdminAccount account = AdminAccountFixture.createWithId(ADMIN_ID, AdminRole.OPERATOR);
        account.recordLoginFailure(NOW, 1, 15);
        when(adminAccountRepository.findByLoginId("ops01")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> adminAuthService.login("ops01", "correct-password"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_ACCOUNT_LOCKED);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("비활성 계정은 비밀번호가 맞아도 로그인할 수 없다")
    void login_disabled_account_throws() {
        AdminAccount account = AdminAccountFixture.createWithId(ADMIN_ID, AdminRole.OPERATOR);
        account.changeStatus(AdminAccountStatus.DISABLED);
        when(adminAccountRepository.findByLoginId("ops01")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("correct-password", account.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> adminAuthService.login("ops01", "correct-password"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_ACCOUNT_DISABLED);
    }

    @Test
    @DisplayName("재발급은 저장된 토큰을 지우고 현재 권한으로 새 토큰을 발급한다")
    void reissue_rotates_tokens_with_current_role() {
        AdminAccount account = AdminAccountFixture.createWithId(ADMIN_ID, AdminRole.SUPER);
        when(adminTokenProvider.parseRefreshToken("refresh")).thenReturn(Optional.of(ADMIN_ID));
        when(adminRefreshTokenRepository.findByToken("refresh"))
                .thenReturn(Optional.of(AdminRefreshToken.issue(ADMIN_ID, "refresh", NOW.plusHours(1))));
        when(adminAccountRepository.findById(ADMIN_ID)).thenReturn(Optional.of(account));
        when(adminTokenProvider.issue(ADMIN_ID, AdminRole.SUPER))
                .thenReturn(new AdminTokenPair("access2", "refresh2", NOW.plusHours(12)));

        AdminLoginResult result = adminAuthService.reissue("refresh");

        assertThat(result.refreshToken()).isEqualTo("refresh2");
        assertThat(result.role()).isEqualTo(AdminRole.SUPER);
        verify(adminRefreshTokenRepository).deleteByToken("refresh");
    }

    @Test
    @DisplayName("만료된 리프레시 토큰으로는 재발급할 수 없다")
    void reissue_expired_token_throws() {
        when(adminTokenProvider.parseRefreshToken("refresh")).thenReturn(Optional.of(ADMIN_ID));
        when(adminRefreshTokenRepository.findByToken("refresh"))
                .thenReturn(Optional.of(AdminRefreshToken.issue(ADMIN_ID, "refresh", NOW.minusMinutes(1))));

        assertThatThrownBy(() -> adminAuthService.reissue("refresh"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("로그아웃은 해당 관리자의 리프레시 토큰을 모두 지운다")
    void logout_deletes_refresh_tokens() {
        adminAuthService.logout(ADMIN_ID);

        verify(adminRefreshTokenRepository).deleteByAdminId(ADMIN_ID);
    }
}
