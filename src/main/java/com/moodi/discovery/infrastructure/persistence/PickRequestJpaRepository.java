package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.domain.PickRequest;
import com.moodi.discovery.domain.PickRequestRepository;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface PickRequestJpaRepository extends PickRequestRepository, Repository<PickRequest, UUID> {
}
