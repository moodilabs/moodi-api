package com.moodi.spot.domain;

import java.util.List;
import java.util.Optional;

public interface SpotTranslationRepository {

    SpotTranslation save(SpotTranslation spotTranslation);

    Optional<SpotTranslation> findBySpotIdAndLocale(Long spotId, String locale);

    List<SpotTranslation> findBySpotIdIn(List<Long> spotIds);
}
