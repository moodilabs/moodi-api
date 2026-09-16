package com.moodi.route.domain;

import java.util.List;
import java.util.Optional;
public interface RecommendedRouteRepository {
    RecommendedRoute save(RecommendedRoute entity);
    Optional<RecommendedRoute> findById(Long id);
    List<RecommendedRoute> findAllByOrderBySortOrderAscIdAsc();
    void delete(RecommendedRoute entity);
}
