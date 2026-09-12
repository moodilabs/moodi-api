package com.moodi.support.infrastructure.persistence;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.support.domain.Policy;
import com.moodi.support.domain.PolicyRepository;
import com.moodi.support.domain.PolicyType;
import com.moodi.support.support.PolicyFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyJpaRepositoryTest extends RepositoryTestSupport {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 10);

    @Autowired
    private PolicyRepository policyRepository;

    @Test
    @DisplayName("현재 적용본은 시행일이 지난 것 중 최신이고, 미래 시행분은 제외한다")
    void find_current_picks_latest_effective_and_ignores_future() {
        policyRepository.save(PolicyFixture.create(PolicyType.TERMS_OF_SERVICE, "1.0", TODAY.minusMonths(6)));
        Policy current = policyRepository.save(PolicyFixture.create(PolicyType.TERMS_OF_SERVICE, "1.1", TODAY.minusDays(3)));
        policyRepository.save(PolicyFixture.create(PolicyType.TERMS_OF_SERVICE, "2.0", TODAY.plusDays(7)));
        policyRepository.save(PolicyFixture.create(PolicyType.PRIVACY_POLICY, "1.0", TODAY.minusDays(1)));

        Optional<Policy> result = policyRepository
                .findFirstByTypeAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(PolicyType.TERMS_OF_SERVICE, TODAY);

        assertThat(result).map(Policy::getId).contains(current.getId());
    }

    @Test
    @DisplayName("같은 시행일이면 나중에 등록한 버전이 적용본이다")
    void find_current_prefers_later_id_on_same_effective_date() {
        policyRepository.save(PolicyFixture.create(PolicyType.PRIVACY_POLICY, "1.0", TODAY));
        Policy later = policyRepository.save(PolicyFixture.create(PolicyType.PRIVACY_POLICY, "1.0-hotfix", TODAY));

        Optional<Policy> result = policyRepository
                .findFirstByTypeAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(PolicyType.PRIVACY_POLICY, TODAY);

        assertThat(result).map(Policy::getId).contains(later.getId());
        assertThat(policyRepository.existsByTypeAndVersion(PolicyType.PRIVACY_POLICY, "1.0")).isTrue();
        assertThat(policyRepository.existsByTypeAndVersion(PolicyType.TERMS_OF_SERVICE, "1.0")).isFalse();
    }
}
