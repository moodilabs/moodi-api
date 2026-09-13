package com.moodi.admin.application;

import com.moodi.admin.application.dto.AdminAccountCommand;
import com.moodi.admin.domain.AdminAccount;
import com.moodi.admin.domain.AdminAccountRepository;
import com.moodi.admin.domain.AdminAccountStatus;
import com.moodi.admin.domain.AdminRefreshTokenRepository;
import com.moodi.admin.support.AdminAccountFixture;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();

    @Mock
    private AdminAccountRepository adminAccountRepository;

    @Mock
    private AdminRefreshTokenRepository adminRefreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminAccountService adminAccountService;

    @Test
    @DisplayName("계정 생성 시 비밀번호를 해시하고 아이디를 소문자로 저장한다")
    void create_hashes_password_and_normalizes_login_id() {
        when(adminAccountRepository.existsByLoginId("newops")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("hashed");
        when(adminAccountRepository.save(any(AdminAccount.class)))
                .thenReturn(AdminAccountFixture.createWithId(TARGET_ID, AdminRole.OPERATOR));

        UUID id = adminAccountService.create(new AdminAccountCommand(" NewOps ", "strong-password", "신규",
                AdminRole.OPERATOR));

        ArgumentCaptor<AdminAccount> captor = ArgumentCaptor.forClass(AdminAccount.class);
        verify(adminAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getLoginId()).isEqualTo("newops");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(id).isEqualTo(TARGET_ID);
    }

    @Test
    @DisplayName("10자 미만 비밀번호로는 계정을 만들 수 없다")
    void create_rejects_short_password() {
        assertThatThrownBy(() -> adminAccountService.create(new AdminAccountCommand("newops", "short", "신규",
                AdminRole.OPERATOR)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(adminAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 사용 중인 아이디로는 계정을 만들 수 없다")
    void create_rejects_duplicate_login_id() {
        when(adminAccountRepository.existsByLoginId("ops01")).thenReturn(true);

        assertThatThrownBy(() -> adminAccountService.create(new AdminAccountCommand("ops01",
                "strong-password", "운영자", AdminRole.OPERATOR)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_ADMIN_LOGIN_ID);
    }

    @Test
    @DisplayName("비활성화하면 리프레시 토큰도 지운다")
    void disable_revokes_refresh_tokens() {
        AdminAccount target = AdminAccountFixture.createWithId(TARGET_ID, AdminRole.OPERATOR);
        when(adminAccountRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        adminAccountService.changeStatus(ACTOR_ID, TARGET_ID, AdminAccountStatus.DISABLED);

        assertThat(target.isActive()).isFalse();
        verify(adminRefreshTokenRepository).deleteByAdminId(TARGET_ID);
    }

    @Test
    @DisplayName("자기 자신은 비활성화할 수 없다")
    void cannot_disable_self() {
        assertThatThrownBy(() -> adminAccountService.changeStatus(ACTOR_ID, ACTOR_ID, AdminAccountStatus.DISABLED))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(adminAccountRepository, never()).findById(any());
    }

    @Test
    @DisplayName("권한을 바꾸면 리프레시 토큰을 끊어 다음 재발급부터 새 권한을 받게 한다")
    void change_role_revokes_refresh_tokens() {
        AdminAccount target = AdminAccountFixture.createWithId(TARGET_ID, AdminRole.OPERATOR);
        when(adminAccountRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        adminAccountService.changeRole(ACTOR_ID, TARGET_ID, AdminRole.SUPER);

        assertThat(target.getRole()).isEqualTo(AdminRole.SUPER);
        verify(adminRefreshTokenRepository).deleteByAdminId(TARGET_ID);
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 비밀번호를 바꿀 수 없다")
    void change_password_with_wrong_current_throws() {
        AdminAccount account = AdminAccountFixture.createWithId(ACTOR_ID, AdminRole.OPERATOR);
        when(adminAccountRepository.findById(ACTOR_ID)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", account.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> adminAccountService.changePassword(ACTOR_ID, "wrong", "new-strong-password"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ADMIN_LOGIN_FAILED);
    }

    @Test
    @DisplayName("비밀번호를 바꾸면 초기 비밀번호 상태가 풀리고 리프레시 토큰을 끊는다")
    void change_password_clears_required_and_revokes_tokens() {
        AdminAccount account = AdminAccountFixture.createWithId(ACTOR_ID, AdminRole.OPERATOR);
        when(adminAccountRepository.findById(ACTOR_ID)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("initial-password", account.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("new-strong-password")).thenReturn("new-hash");

        adminAccountService.changePassword(ACTOR_ID, "initial-password", "new-strong-password");

        assertThat(account.isPasswordChangeRequired()).isFalse();
        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
        verify(adminRefreshTokenRepository).deleteByAdminId(ACTOR_ID);
    }

    @Test
    @DisplayName("초기 비밀번호를 그대로 다시 넣어 강제 변경을 우회할 수 없다")
    void change_password_rejects_same_password() {
        AdminAccount account = AdminAccountFixture.createWithId(ACTOR_ID, AdminRole.OPERATOR);
        when(adminAccountRepository.findById(ACTOR_ID)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("initial-password", account.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> adminAccountService.changePassword(ACTOR_ID, "initial-password", "initial-password"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(adminAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("최초 부트스트랩은 계정이 없을 때만 SUPER를 만든다")
    void bootstrap_creates_super_only_when_empty() {
        when(adminAccountRepository.count()).thenReturn(0L, 1L);
        when(passwordEncoder.encode("bootstrap-password")).thenReturn("hashed");

        boolean first = adminAccountService.bootstrap("root", "bootstrap-password");
        boolean second = adminAccountService.bootstrap("root", "bootstrap-password");

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        ArgumentCaptor<AdminAccount> captor = ArgumentCaptor.forClass(AdminAccount.class);
        verify(adminAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(AdminRole.SUPER);
    }
}
