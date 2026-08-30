package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.domain.PickResultSpot;
import com.moodi.discovery.domain.PickResultSpotRepository;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface PickResultSpotJpaRepository
        extends PickResultSpotRepository, Repository<PickResultSpot, UUID> {
}
