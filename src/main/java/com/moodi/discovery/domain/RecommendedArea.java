package com.moodi.discovery.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendedArea extends BaseEntity {

    private Long id;
    private PickAreaLevel level;
    private String region;
    private String district;
    private String neighborhood;
    private String label;
    private int sortOrder;

    public static RecommendedArea create(PickAreaLevel level, String region, String district, String neighborhood, String label, int sortOrder) {
        RecommendedArea area = new RecommendedArea();
        area.update(level, region, district, neighborhood, label, sortOrder);
        return area;
    }

    public void update(PickAreaLevel level, String region, String district, String neighborhood, String label, int sortOrder) {
        new PickArea(level, region, district, neighborhood);
        if (label == null || label.isBlank() || label.length() > 200 || region.length() > 100
                || (district != null && district.length() > 100) || (neighborhood != null && neighborhood.length() > 100) || sortOrder < 0)
        throw new BusinessException(ErrorCode.INVALID_REQUEST);
        this.level = level;
        this.region = region;
        this.district = district;
        this.neighborhood = neighborhood;
        this.label = label;
        this.sortOrder = sortOrder;
    }

    public void reorder(int order) {
        this.sortOrder = order;
    }
}
