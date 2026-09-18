package com.moodi.support.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.support.PolicyFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 10);

    @Test
    @DisplayName("시행일이 오늘이거나 지났으면 시행 중이다")
    void is_effective_when_effective_at_is_today_or_past() {
        assertThat(PolicyFixture.create(PolicyType.TERMS_OF_SERVICE, "1.0", TODAY).isEffective(TODAY)).isTrue();
        assertThat(PolicyFixture.create(PolicyType.TERMS_OF_SERVICE, "1.0", TODAY.minusDays(1)).isEffective(TODAY)).isTrue();
        assertThat(PolicyFixture.create(PolicyType.TERMS_OF_SERVICE, "1.0", TODAY.plusDays(1)).isEffective(TODAY)).isFalse();
    }

    @Test
    @DisplayName("동의한 회원이 없으면 시행 중이어도 수정할 수 있다")
    void update_allowed_when_nobody_agreed() {
        Policy policy = PolicyFixture.create(PolicyType.PRIVACY_POLICY, "1.1", TODAY.minusDays(7));

        policy.update("1.2", "new content", TODAY.plusDays(14), false);

        assertThat(policy.getVersion()).isEqualTo("1.2");
        assertThat(policy.getContent()).isEqualTo("new content");
        assertThat(policy.getEffectiveAt()).isEqualTo(TODAY.plusDays(14));
    }

    @Test
    @DisplayName("회원이 동의한 버전은 수정할 수 없다")
    void update_rejected_when_agreed() {
        Policy policy = PolicyFixture.create(PolicyType.PRIVACY_POLICY, "1.0", TODAY);

        assertThatThrownBy(() -> policy.update("1.1", "fix typo", TODAY, true))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.POLICY_ALREADY_AGREED);
        assertThat(policy.getVersion()).isEqualTo("1.0");
    }

    @Test
    @DisplayName("버전이 비어 있으면 생성할 수 없다")
    void create_rejects_blank_version() {
        assertThatThrownBy(() -> Policy.create(PolicyType.TERMS_OF_SERVICE, " ", "content", TODAY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
