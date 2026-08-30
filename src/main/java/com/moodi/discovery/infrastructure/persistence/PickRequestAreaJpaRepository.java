package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.domain.PickRequestArea;
import com.moodi.discovery.domain.PickRequestAreaRepository;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface PickRequestAreaJpaRepository
        extends PickRequestAreaRepository, Repository<PickRequestArea, UUID> {
}
