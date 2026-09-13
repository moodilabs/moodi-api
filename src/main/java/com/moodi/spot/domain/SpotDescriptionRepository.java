package com.moodi.spot.domain;

import java.util.List;
import java.util.Optional;

public interface SpotDescriptionRepository {

    Optional<SpotDescription> findBySpotIdAndLocale(Long spotId, String locale);

    SpotDescription saveIfAbsent(SpotDescription description);

    /** 어드민 수정용. 이미 있는 행을 갱신하거나 없으면 새로 넣는다. */
    SpotDescription save(SpotDescription description);

    List<SpotDescription> findBySpotIdInAndLocale(List<Long> spotIds, String locale);

    List<Long> findSpotIdsWithoutDescription(String locale);
}
