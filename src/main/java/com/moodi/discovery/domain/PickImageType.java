package com.moodi.discovery.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

import java.util.Arrays;

/**
 * Pick 추천에 업로드할 수 있는 이미지 형식 (DSC-04).
 *
 * <p>{@code spot}의 {@code GcsSpotImageUploader}는 TourAPI 이미지를 다루느라 webp를 허용하고 heic를 모른다.
 * Pick은 사용자 기기의 사진을 받으므로 허용 목록이 다르다. 그래서 그쪽을 재사용하지 않고 여기서 새로 정의한다.
 */
public enum PickImageType {

    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    HEIC("image/heic", "heic");

    private final String contentType;
    private final String extension;

    PickImageType(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public static PickImageType from(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException(ErrorCode.PICK_UNSUPPORTED_IMAGE_TYPE);
        }
        String normalized = contentType.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(type -> type.contentType.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PICK_UNSUPPORTED_IMAGE_TYPE));
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }
}
