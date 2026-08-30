package com.moodi.discovery.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

import java.util.Objects;

/**
 * 선택된 지역 1개. 자동완성 결과에서 고른 값만 들어오므로(COM-P03) 하위 단계가 있으면
 * 상위 단계도 반드시 채워져 있어야 한다.
 */
public record PickArea(PickAreaLevel level, String region, String district, String neighborhood) {

    public PickArea {
        if (level == null || isBlank(region)) {
            throw new BusinessException(ErrorCode.PICK_INVALID_AREA_SELECTION);
        }
        boolean districtRequired = level != PickAreaLevel.REGION;
        if (districtRequired == isBlank(district)) {
            throw new BusinessException(ErrorCode.PICK_INVALID_AREA_SELECTION);
        }
        boolean neighborhoodRequired = level == PickAreaLevel.NEIGHBORHOOD;
        if (neighborhoodRequired == isBlank(neighborhood)) {
            throw new BusinessException(ErrorCode.PICK_INVALID_AREA_SELECTION);
        }
    }

    /**
     * 이 지역이 {@code other}를 포함하는지. 상위 지역을 고르면 그 안의 하위 지역은 이미 포함된다.
     */
    public boolean contains(PickArea other) {
        if (!Objects.equals(region, other.region)) {
            return false;
        }
        if (level == PickAreaLevel.REGION) {
            return true;
        }
        if (!Objects.equals(district, other.district)) {
            return false;
        }
        return level == PickAreaLevel.DISTRICT || Objects.equals(neighborhood, other.neighborhood);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
