package com.moodi.support.infrastructure.persistence;

import com.moodi.support.domain.Policy;
import com.moodi.support.domain.PolicyRepository;
import org.springframework.data.repository.Repository;

public interface PolicyJpaRepository extends PolicyRepository, Repository<Policy, Long> {
    @Override
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Policy p WHERE p.type = :type AND p.locale = 'en-US' AND p.enabled = true AND p.visible = true AND p.effectiveAt <= :today ORDER BY p.effectiveAt DESC, p.id DESC LIMIT 1")
    java.util.Optional<Policy> findFirstByTypeAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(
            com.moodi.support.domain.PolicyType type, java.time.LocalDate today);

}
