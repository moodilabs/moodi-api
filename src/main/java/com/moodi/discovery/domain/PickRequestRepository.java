package com.moodi.discovery.domain;

import java.util.Optional;
import java.util.UUID;

public interface PickRequestRepository {

    PickRequest save(PickRequest pickRequest);

    Optional<PickRequest> findById(UUID id);
}
