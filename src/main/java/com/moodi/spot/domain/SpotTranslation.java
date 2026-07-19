package com.moodi.spot.domain;

import com.moodi.shared.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotTranslation extends BaseEntity {

    private Long id;
    private Long spotId;
    private String locale;
    private String title;
    private String overview;
    private String addr1;
    private String addr2;

    private SpotTranslation(Long spotId, String locale, String title, String overview,
                            String addr1, String addr2) {
        this.spotId = spotId;
        this.locale = locale;
        this.title = title;
        this.overview = overview;
        this.addr1 = addr1;
        this.addr2 = addr2;
    }

    public static SpotTranslation create(Long spotId, String locale, String title, String overview,
                                         String addr1, String addr2) {
        return new SpotTranslation(spotId, locale, title, overview, addr1, addr2);
    }
}
