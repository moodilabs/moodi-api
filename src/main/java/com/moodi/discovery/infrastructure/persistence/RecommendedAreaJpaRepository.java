package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.domain.RecommendedArea;
import com.moodi.discovery.domain.RecommendedAreaRepository;
import org.springframework.data.repository.Repository;
public interface RecommendedAreaJpaRepository extends RecommendedAreaRepository, Repository<RecommendedArea, Long> {}
