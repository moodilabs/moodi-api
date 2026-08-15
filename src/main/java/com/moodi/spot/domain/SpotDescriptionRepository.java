package com.moodi.spot.domain;

import java.util.List;
import java.util.Optional;

public interface SpotDescriptionRepository {

    Optional<SpotDescription> findBySpotIdAndLocale(Long spotId, String locale);

    SpotDescription saveIfAbsent(SpotDescription description);

    List<Long> findSpotIdsWithoutDescription(String locale);
}
