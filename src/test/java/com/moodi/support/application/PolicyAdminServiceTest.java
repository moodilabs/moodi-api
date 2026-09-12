package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.application.dto.PolicyCommand;
import com.moodi.support.domain.Policy;
import com.moodi.support.domain.PolicyRepository;
import com.moodi.support.domain.PolicyType;
import com.moodi.support.support.PolicyFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyAdminServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 10);

    @Mock
    private PolicyRepository policyRepository;

    private PolicyAdminService policyAdminService;

    @BeforeEach
    void setUp() {
        policyAdminService = new PolicyAdminService(policyRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("새 버전을 등록하면 ID를 돌려준다")
    void create_returns_id() {
        when(policyRepository.existsByTypeAndVersion(PolicyType.TERMS_OF_SERVICE, "2.0")).thenReturn(false);
        when(policyRepository.save(any(Policy.class)))
                .thenReturn(PolicyFixture.createWithId(5L, PolicyType.TERMS_OF_SERVICE, "2.0", TODAY.plusDays(7)));

        Long id = policyAdminService.create(new PolicyCommand(PolicyType.TERMS_OF_SERVICE, "2.0", "content",
                TODAY.plusDays(7)));

        assertThat(id).isEqualTo(5L);
        verify(policyRepository).flush();
    }

    @Test
    @DisplayName("같은 종류에 같은 버전이 있으면 등록할 수 없다")
    void create_with_duplicate_version_throws() {
        when(policyRepository.existsByTypeAndVersion(PolicyType.TERMS_OF_SERVICE, "1.0")).thenReturn(true);

        assertThatThrownBy(() -> policyAdminService.create(new PolicyCommand(PolicyType.TERMS_OF_SERVICE, "1.0",
                "content", TODAY)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.POLICY_VERSION_DUPLICATE);
        verify(policyRepository, never()).save(any());
    }

    @Test
    @DisplayName("저장 중 버전이 선점되면 중복으로 처리한다")
    void create_with_race_condition_throws_duplicate() {
        when(policyRepository.existsByTypeAndVersion(PolicyType.TERMS_OF_SERVICE, "2.0")).thenReturn(false);
        when(policyRepository.save(any(Policy.class)))
                .thenReturn(PolicyFixture.createWithId(5L, PolicyType.TERMS_OF_SERVICE, "2.0", TODAY));
        doThrow(new DataIntegrityViolationException("uk_policy_type_version")).when(policyRepository).flush();

        assertThatThrownBy(() -> policyAdminService.create(new PolicyCommand(PolicyType.TERMS_OF_SERVICE, "2.0",
                "content", TODAY)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.POLICY_VERSION_DUPLICATE);
    }

    @Test
    @DisplayName("시행 전 버전은 수정된다")
    void update_before_effective_succeeds() {
        Policy policy = PolicyFixture.createWithId(1L, PolicyType.PRIVACY_POLICY, "1.1", TODAY.plusDays(7));
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(policyRepository.save(policy)).thenReturn(policy);

        policyAdminService.update(1L, new PolicyCommand(PolicyType.PRIVACY_POLICY, "1.1", "revised",
                TODAY.plusDays(10)));

        assertThat(policy.getContent()).isEqualTo("revised");
        assertThat(policy.getEffectiveAt()).isEqualTo(TODAY.plusDays(10));
    }

    @Test
    @DisplayName("이미 시행된 버전은 삭제할 수 없다")
    void delete_effective_policy_throws() {
        when(policyRepository.findById(1L))
                .thenReturn(Optional.of(PolicyFixture.createWithId(1L, PolicyType.PRIVACY_POLICY, "1.0", TODAY)));

        assertThatThrownBy(() -> policyAdminService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.POLICY_ALREADY_EFFECTIVE);
        verify(policyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("시행 전 버전은 삭제된다")
    void delete_before_effective_succeeds() {
        Policy policy = PolicyFixture.createWithId(1L, PolicyType.PRIVACY_POLICY, "1.1", TODAY.plusDays(1));
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));

        policyAdminService.delete(1L);

        verify(policyRepository).delete(policy);
    }
}
