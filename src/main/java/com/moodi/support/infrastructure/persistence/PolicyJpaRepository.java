package com.moodi.support.infrastructure.persistence;

import com.moodi.support.domain.Policy;
import com.moodi.support.domain.PolicyRepository;
import org.springframework.data.repository.Repository;

public interface PolicyJpaRepository extends PolicyRepository, Repository<Policy, Long> {
}
