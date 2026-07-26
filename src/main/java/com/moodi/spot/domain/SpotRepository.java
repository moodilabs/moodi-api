package com.moodi.spot.domain;

import java.util.List;
import java.util.Optional;

public interface SpotRepository {

    Spot save(Spot spot);

    Optional<Spot> findById(Long id);

    Optional<Spot> findBySourceAndContentId(String source, String contentId);

    boolean existsBySourceAndContentId(String source, String contentId);

    List<Spot> findByStatus(SpotStatus status);

    List<Spot> findByDistrictIsNull();
}
