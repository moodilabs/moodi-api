package com.moodi.spot.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotImage extends BaseEntity {

    private Long id;
    private Long spotId;
    private String imageUrl;
    private boolean isPrimary;
    private int sortOrder;

    private SpotImage(Long spotId, String imageUrl, boolean isPrimary, int sortOrder) {
        this.spotId = spotId;
        this.imageUrl = imageUrl;
        this.isPrimary = isPrimary;
        this.sortOrder = sortOrder;
    }

    public static SpotImage createPrimary(Long spotId, String imageUrl) {
        return new SpotImage(spotId, imageUrl, true, 0);
    }
}
