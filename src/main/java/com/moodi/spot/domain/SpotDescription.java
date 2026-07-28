package com.moodi.spot.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotDescription extends BaseEntity {

    private Long id;
    private Long spotId;
    private String locale;
    private String content;

    private SpotDescription(Long spotId, String locale, String content) {
        this.spotId = spotId;
        this.locale = locale;
        this.content = content;
    }

    public static SpotDescription create(Long spotId, String locale, String content) {
        return new SpotDescription(spotId, locale, content);
    }
}
