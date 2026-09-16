package com.moodi.member.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyImage extends BaseEntity {

    private Long id;
    private MoodTag mood;
    private Long spotId;
    private String imageUrl;
    private int sortOrder;

    public static SurveyImage create(MoodTag mood, Long spotId, String imageUrl, int sortOrder) {
        SurveyImage image = new SurveyImage();
        image.update(mood, spotId, imageUrl, sortOrder);
        return image;
    }

    public void update(MoodTag mood, Long spotId, String imageUrl, int sortOrder) {
        if (mood == null || spotId == null || spotId <= 0 || imageUrl == null || !imageUrl.matches("https?://[^\\s]+")
                || imageUrl.length() > 2000 || sortOrder < 0) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        this.mood = mood;
        this.spotId = spotId;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    public void reorder(int order) {
        this.sortOrder = order;
    }
}
