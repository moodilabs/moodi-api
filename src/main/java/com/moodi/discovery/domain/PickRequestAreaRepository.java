package com.moodi.discovery.domain;

import java.util.List;
import java.util.UUID;

public interface PickRequestAreaRepository {

    PickRequestArea save(PickRequestArea area);

    List<PickRequestArea> findByPickRequestIdOrderBySortOrder(UUID pickRequestId);
}
