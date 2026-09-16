package com.moodi.discovery.domain;

import java.util.List;
import java.util.Optional;
public interface RecommendedAreaRepository {
    RecommendedArea save(RecommendedArea entity);
    Optional<RecommendedArea> findById(Long id);
    List<RecommendedArea> findAllByOrderBySortOrderAscIdAsc();
    void delete(RecommendedArea entity);
}
