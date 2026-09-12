package com.moodi.support.infrastructure.persistence;

import com.moodi.support.domain.FaqCategory;
import com.moodi.support.domain.FaqCategoryRepository;
import org.springframework.data.repository.Repository;

public interface FaqCategoryJpaRepository extends FaqCategoryRepository, Repository<FaqCategory, Long> {
}
