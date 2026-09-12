package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.application.dto.PolicyDetail;
import com.moodi.support.application.dto.PolicySummary;
import com.moodi.support.domain.PolicyRepository;
import com.moodi.support.domain.PolicyType;
import com.moodi.support.support.PolicyFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyQueryServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 10);

    @Mock
    private PolicyRepository policyRepository;

    private PolicyQueryService policyQueryService;

    @BeforeEach
    void setUp() {
        policyQueryService = new PolicyQueryService(policyRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("종류별 현재 적용본을 enum 순서로 돌려주고 적용본이 없는 종류는 뺀다")
    void get_current_policies_returns_effective_per_type() {
        when(policyRepository.findFirstByTypeAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(
                PolicyType.TERMS_OF_SERVICE, TODAY))
                .thenReturn(Optional.of(PolicyFixture.createWithId(1L, PolicyType.TERMS_OF_SERVICE, "1.2",
                        LocalDate.of(2026, 8, 1))));
        when(policyRepository.findFirstByTypeAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(
                PolicyType.PRIVACY_POLICY, TODAY))
                .thenReturn(Optional.empty());

        List<PolicySummary> result = policyQueryService.getCurrentPolicies();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(PolicyType.TERMS_OF_SERVICE);
        assertThat(result.getFirst().version()).isEqualTo("1.2");
    }

    @Test
    @DisplayName("현재 적용본 전문을 돌려준다")
    void get_current_policy_returns_detail() {
        when(policyRepository.findFirstByTypeAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(
                PolicyType.PRIVACY_POLICY, TODAY))
                .thenReturn(Optional.of(PolicyFixture.createWithId(2L, PolicyType.PRIVACY_POLICY, "1.0", TODAY)));

        PolicyDetail detail = policyQueryService.getCurrentPolicy(PolicyType.PRIVACY_POLICY);

        assertThat(detail.content()).isEqualTo(PolicyFixture.DEFAULT_CONTENT);
    }

    @Test
    @DisplayName("적용본이 없으면 조회에 실패한다")
    void get_current_policy_without_effective_version_throws() {
        when(policyRepository.findFirstByTypeAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(
                PolicyType.PRIVACY_POLICY, TODAY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyQueryService.getCurrentPolicy(PolicyType.PRIVACY_POLICY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.POLICY_NOT_FOUND);
    }
}
