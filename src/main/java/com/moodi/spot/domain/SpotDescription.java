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

    /** 어드민 수동 수정. AI 생성 설명을 사람이 고친다. */
    public void updateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new com.moodi.shared.error.BusinessException(com.moodi.shared.error.ErrorCode.INVALID_REQUEST);
        }
        this.content = content.trim();
    }
}
