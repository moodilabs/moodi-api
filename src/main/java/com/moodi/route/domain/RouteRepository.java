package com.moodi.route.domain;

import java.util.Optional;
import java.util.UUID;

public interface RouteRepository {

    Route save(Route route);

    Optional<Route> findById(Long id);

    Optional<Route> findByPublicId(UUID publicId);

    void delete(Route route);
}
