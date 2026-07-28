package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
import org.springframework.data.repository.Repository;

public interface SpotDescriptionJpaRepository extends SpotDescriptionRepository, Repository<SpotDescription, Long> {
}
