package com.moodi.admin.domain;

import com.moodi.admin.support.AdminAccountFixture;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAccountTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 10, 10, 0);

    @Test
    @DisplayName("생성 시 ACTIVE이고 초기 비밀번호 변경 대상이다")
    void create_activates_and_requires_password_change() {
        AdminAccount account = AdminAccount.create("ops_01", "hash", "운영자", AdminRole.OPERATOR);

        assertThat(account.getLoginId()).isEqualTo("ops_01");
        assertThat(account.isActive()).isTrue();
        assertThat(account.isPasswordChangeRequired()).isTrue();
        assertThat(account.getFailedLoginCount()).isZero();
    }

    @Test
    @DisplayName("아이디가 영문·숫자·밑줄 4~20자가 아니면 생성할 수 없다")
    void create_rejects_invalid_login_id() {
        for (String invalid : new String[]{"ab", "ops@moodi.kr", "Ops", "한글아이디", "a".repeat(21)}) {
            assertThatThrownBy(() -> AdminAccount.create(invalid, "hash", "운영자", AdminRole.OPERATOR))
                    .as(invalid)
                    .isInstanceOf(BusinessException.class)
                    .extracting(exception -> ((BusinessException) exception).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }

    @Test
    @DisplayName("비밀번호를 바꾸면 초기 비밀번호 변경 대상에서 풀린다")
    void change_password_clears_required_flag() {
        AdminAccount account = AdminAccountFixture.create();

        account.changePassword("new-hash");

        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
        assertThat(account.isPasswordChangeRequired()).isFalse();
    }

    @Test
    @DisplayName("실패가 한도에 도달하면 잠기고 카운트가 초기화된다")
    void lock_after_max_failures() {
        AdminAccount account = AdminAccountFixture.create();

        account.recordLoginFailure(NOW, 3, 15);
        account.recordLoginFailure(NOW, 3, 15);
        assertThat(account.isLocked(NOW)).isFalse();
        assertThat(account.getFailedLoginCount()).isEqualTo(2);

        account.recordLoginFailure(NOW, 3, 15);

        assertThat(account.isLocked(NOW)).isTrue();
        assertThat(account.isLocked(NOW.plusMinutes(15))).isFalse();
        assertThat(account.getFailedLoginCount()).isZero();
    }

    @Test
    @DisplayName("로그인 성공 시 실패 카운트와 잠금이 풀리고 마지막 로그인이 기록된다")
    void success_resets_failures_and_lock() {
        AdminAccount account = AdminAccountFixture.create();
        account.recordLoginFailure(NOW, 1, 15);
        assertThat(account.isLocked(NOW)).isTrue();

        account.recordLoginSuccess(NOW.plusMinutes(20));

        assertThat(account.isLocked(NOW.plusMinutes(20))).isFalse();
        assertThat(account.getFailedLoginCount()).isZero();
        assertThat(account.getLastLoginAt()).isEqualTo(NOW.plusMinutes(20));
    }

    @Test
    @DisplayName("SUPER는 OPERATOR 권한을 포함한다")
    void super_covers_operator() {
        assertThat(AdminRole.SUPER.covers(AdminRole.OPERATOR)).isTrue();
        assertThat(AdminRole.SUPER.covers(AdminRole.SUPER)).isTrue();
        assertThat(AdminRole.OPERATOR.covers(AdminRole.SUPER)).isFalse();
        assertThat(AdminRole.OPERATOR.covers(AdminRole.OPERATOR)).isTrue();
    }
}
