package com.moodi.route.domain;

import java.util.Optional;
import java.util.UUID;

public interface RouteRepository {

    Route save(Route route);

    Optional<Route> findById(Long id);

    Optional<Route> findByPublicId(UUID publicId);

    Optional<Route> findByPublicIdWithDays(UUID publicId);

    Optional<Route> findSharedByPublicId(UUID publicId);

    Optional<Route> findSharedByPublicIdWithDays(UUID publicId);

    void delete(Route route);
}
