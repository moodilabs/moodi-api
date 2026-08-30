package com.moodi.discovery.domain;

import java.util.List;
import java.util.UUID;

public interface PickResultSpotRepository {

    PickResultSpot save(PickResultSpot result);

    List<PickResultSpot> findByPickRequestIdOrderByRank(UUID pickRequestId);
}
