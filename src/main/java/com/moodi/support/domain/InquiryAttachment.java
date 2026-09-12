package com.moodi.support.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 문의에 붙은 파일 1개. 비공개 버킷의 객체 키만 갖고, 읽기 URL은 조회 시점에 발급한다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryAttachment {

    private String objectKey;
    private String contentType;
    private int sortOrder;

    private InquiryAttachment(String objectKey, String contentType, int sortOrder) {
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.sortOrder = sortOrder;
    }

    public static InquiryAttachment of(String objectKey, String contentType, int sortOrder) {
        return new InquiryAttachment(objectKey, contentType, sortOrder);
    }
}
