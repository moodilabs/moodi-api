package com.moodi.discovery.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PickRequestArea extends BaseEntity {

    private UUID id;
    private UUID pickRequestId;
    private PickAreaLevel level;
    private String region;
    private String district;
    private String neighborhood;
    private int sortOrder;

    private PickRequestArea(UUID pickRequestId, PickArea area, int sortOrder) {
        this.pickRequestId = pickRequestId;
        this.level = area.level();
        this.region = area.region();
        this.district = area.district();
        this.neighborhood = area.neighborhood();
        this.sortOrder = sortOrder;
    }

    public static PickRequestArea of(UUID pickRequestId, PickArea area, int sortOrder) {
        return new PickRequestArea(pickRequestId, area, sortOrder);
    }

    public PickArea toArea() {
        return new PickArea(level, region, district, neighborhood);
    }
}
