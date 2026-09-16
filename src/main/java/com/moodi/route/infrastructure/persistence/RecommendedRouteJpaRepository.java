package com.moodi.route.infrastructure.persistence;

import com.moodi.route.domain.RecommendedRoute;
import com.moodi.route.domain.RecommendedRouteRepository;
import org.springframework.data.repository.Repository;
public interface RecommendedRouteJpaRepository extends RecommendedRouteRepository, Repository<RecommendedRoute, Long> {}
