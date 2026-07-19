package com.moodi.spot.domain;

import java.util.Optional;

public interface SpotRepository {

    Spot save(Spot spot);

    Optional<Spot> findBySourceAndContentId(String source, String contentId);

    boolean existsBySourceAndContentId(String source, String contentId);
}
