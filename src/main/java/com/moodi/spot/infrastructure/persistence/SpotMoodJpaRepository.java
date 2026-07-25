package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.domain.SpotMoodRepository;
import org.springframework.data.repository.Repository;

public interface SpotMoodJpaRepository extends SpotMoodRepository, Repository<SpotMood, Long> {
}
