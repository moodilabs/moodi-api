package com.moodi.support.infrastructure.persistence;

import com.moodi.support.domain.Faq;
import com.moodi.support.domain.FaqRepository;
import org.springframework.data.repository.Repository;

public interface FaqJpaRepository extends FaqRepository, Repository<Faq, Long> {
}
