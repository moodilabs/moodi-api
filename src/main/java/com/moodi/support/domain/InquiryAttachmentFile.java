package com.moodi.support.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

import java.util.UUID;

/**
 * 업로드하려는 첨부 1개. 서명 URL을 발급하는 순간 그 URL로 무엇이든 올라갈 수 있으므로
 * 발급 전에 형식·용량을 서버가 확정한다 ({@code discovery.PickImage}와 같은 이유).
 */
public class InquiryAttachmentFile {

    private static final String KEY_PREFIX = "inquiries/";

    private final InquiryAttachmentType type;
    private final long contentLength;

    private InquiryAttachmentFile(InquiryAttachmentType type, long contentLength) {
        this.type = type;
        this.contentLength = contentLength;
    }

    public static InquiryAttachmentFile of(String contentType, long contentLength) {
        InquiryAttachmentType type = InquiryAttachmentType.from(contentType);
        if (contentLength <= 0 || contentLength > type.getMaxBytes()) {
            throw new BusinessException(ErrorCode.INQUIRY_ATTACHMENT_TOO_LARGE);
        }
        return new InquiryAttachmentFile(type, contentLength);
    }

    /** 회원별 경로. 파일명은 UUID라 원본 파일명이 노출되지 않는다. */
    public String objectName(UUID memberId) {
        return keyPrefix(memberId) + UUID.randomUUID() + "." + type.getExtension();
    }

    /** 문의 등록 시 넘어온 키가 본인 경로인지 확인하는 데 쓴다 — 남의 객체를 자기 문의에 붙이지 못하게. */
    public static String keyPrefix(UUID memberId) {
        return KEY_PREFIX + memberId + "/";
    }

    public InquiryAttachmentType getType() {
        return type;
    }

    public long getContentLength() {
        return contentLength;
    }
}
