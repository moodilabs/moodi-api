package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
import org.springframework.data.repository.Repository;

public interface SpotTranslationJpaRepository extends SpotTranslationRepository, Repository<SpotTranslation, Long> {
}
