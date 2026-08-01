package com.moodi.route.infrastructure.persistence;

import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteRepository;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface RouteJpaRepository extends RouteRepository, Repository<Route, Long> {

    @Override
    Route save(Route route);

    @Override
    Optional<Route> findById(Long id);

    @Override
    Optional<Route> findByPublicId(UUID publicId);

    @Override
    void delete(Route route);
}
