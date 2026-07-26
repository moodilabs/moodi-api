package com.moodi.spot.domain;

import java.util.List;
import java.util.Optional;

public interface SpotMoodRepository {

    SpotMood save(SpotMood spotMood);

    Optional<SpotMood> findBySpotId(Long spotId);

    boolean existsBySpotId(Long spotId);

    List<SpotMood> findBySpotIdIn(List<Long> spotIds);
}
