package com.moodi.spot.domain;

import java.util.Optional;

public interface SpotTranslationRepository {

    SpotTranslation save(SpotTranslation spotTranslation);

    Optional<SpotTranslation> findBySpotIdAndLocale(Long spotId, String locale);
}
