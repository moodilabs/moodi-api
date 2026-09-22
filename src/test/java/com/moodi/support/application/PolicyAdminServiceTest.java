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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyAdminServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 10);

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private PolicyAgreementReader policyAgreementReader;

    @Mock
    private HtmlSanitizer htmlSanitizer;

    private PolicyAdminService policyAdminService;

    @BeforeEach
    void setUp() {
        lenient().when(htmlSanitizer.sanitize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        policyAdminService = new PolicyAdminService(policyRepository, policyAgreementReader, htmlSanitizer);
    }

    @Test
    @DisplayName("새 버전을 등록하면 ID를 돌려준다")
    void create_returns_id() {
        when(policyRepository.existsByTypeAndVersionAndLocale(PolicyType.TERMS_OF_SERVICE, "2.0", "en-US")).thenReturn(false);
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
        when(policyRepository.existsByTypeAndVersionAndLocale(PolicyType.TERMS_OF_SERVICE, "1.0", "en-US")).thenReturn(true);

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
        when(policyRepository.existsByTypeAndVersionAndLocale(PolicyType.TERMS_OF_SERVICE, "2.0", "en-US")).thenReturn(false);
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
    @DisplayName("회원이 동의한 시행 중인 버전도 수정된다")
    void update_agreed_effective_policy_succeeds() {
        Policy policy = PolicyFixture.createWithId(1L, PolicyType.PRIVACY_POLICY, "1.1", TODAY.minusDays(7));
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(policyRepository.save(policy)).thenReturn(policy);

        policyAdminService.update(1L, new PolicyCommand(PolicyType.PRIVACY_POLICY, "1.1", "revised",
                TODAY.plusDays(10)));

        assertThat(policy.getContent()).isEqualTo("revised");
        assertThat(policy.getEffectiveAt()).isEqualTo(TODAY.plusDays(10));
    }

    @Test
    @DisplayName("회원이 동의한 시행 중인 버전도 삭제된다")
    void delete_agreed_effective_policy_succeeds() {
        Policy policy = PolicyFixture.createWithId(1L, PolicyType.PRIVACY_POLICY, "1.0", TODAY.minusDays(1));
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));

        policyAdminService.delete(1L);

        verify(policyRepository).delete(policy);
    }
}
