package com.moodi.discovery.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

import java.util.UUID;

/**
 * 업로드하려는 Pick 사진 1장. 화면정의서 DSC-04의 파일 정책(형식·용량)을 불변식으로 갖는다.
 *
 * <p>클라이언트도 같은 검증을 하지만 신뢰하지 않는다. 서명 URL을 발급하는 순간
 * 그 URL로 무엇이든 올라갈 수 있으므로, 발급 전에 서버가 형식과 용량을 확정해야 한다.
 */
public class PickImage {

    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private final PickImageType type;
    private final long contentLength;

    private PickImage(PickImageType type, long contentLength) {
        this.type = type;
        this.contentLength = contentLength;
    }

    public static PickImage of(String contentType, long contentLength) {
        PickImageType type = PickImageType.from(contentType);
        if (contentLength <= 0 || contentLength > MAX_BYTES) {
            throw new BusinessException(ErrorCode.PICK_IMAGE_TOO_LARGE);
        }
        return new PickImage(type, contentLength);
    }

    /**
     * 회원별 경로로 나눠 저장한다. 파일명은 UUID라 원본 파일명이 노출되지 않는다.
     */
    public String objectName(UUID memberId) {
        return "picks/%s/%s.%s".formatted(memberId, UUID.randomUUID(), type.getExtension());
    }

    public PickImageType getType() {
        return type;
    }

    public long getContentLength() {
        return contentLength;
    }
}
