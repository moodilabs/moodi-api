package com.moodi.spot.domain;

import java.util.List;

public interface SpotImageRepository {

    SpotImage save(SpotImage spotImage);

    List<SpotImage> findBySpotId(Long spotId);
}
