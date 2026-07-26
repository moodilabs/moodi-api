package com.moodi.spot.domain;

import java.util.Optional;

public interface SpotDescriptionRepository {

    Optional<SpotDescription> findBySpotIdAndLocale(Long spotId, String locale);
}
