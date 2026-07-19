package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotRepository;
import org.springframework.data.repository.Repository;

public interface SpotJpaRepository extends SpotRepository, Repository<Spot, Long> {
}
