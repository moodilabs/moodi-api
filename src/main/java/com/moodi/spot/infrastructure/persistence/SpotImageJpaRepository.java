package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import org.springframework.data.repository.Repository;

public interface SpotImageJpaRepository extends SpotImageRepository, Repository<SpotImage, Long> {
}
