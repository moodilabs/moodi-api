package com.moodi.support.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

import java.util.Arrays;

/** 첨부 허용 형식(`MY-06-03`: 사진·동영상·파일). 형식별 용량 상한을 함께 정한다. */
public enum InquiryAttachmentType {

    JPEG("image/jpeg", "jpg", 10L * 1024 * 1024),
    PNG("image/png", "png", 10L * 1024 * 1024),
    HEIC("image/heic", "heic", 10L * 1024 * 1024),
    MP4("video/mp4", "mp4", 50L * 1024 * 1024),
    QUICKTIME("video/quicktime", "mov", 50L * 1024 * 1024),
    PDF("application/pdf", "pdf", 10L * 1024 * 1024);

    private final String contentType;
    private final String extension;
    private final long maxBytes;

    InquiryAttachmentType(String contentType, String extension, long maxBytes) {
        this.contentType = contentType;
        this.extension = extension;
        this.maxBytes = maxBytes;
    }

    public static InquiryAttachmentType from(String contentType) {
        if (contentType == null) {
            throw new BusinessException(ErrorCode.INQUIRY_UNSUPPORTED_ATTACHMENT_TYPE);
        }
        String normalized = contentType.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(type -> type.contentType.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_UNSUPPORTED_ATTACHMENT_TYPE));
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }

    public long getMaxBytes() {
        return maxBytes;
    }
}
