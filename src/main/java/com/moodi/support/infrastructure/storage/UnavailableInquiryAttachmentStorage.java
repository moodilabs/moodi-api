package com.moodi.support.infrastructure.storage;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.application.InquiryAttachmentStorage;

/** 버킷·IAM이 준비되지 않은 환경. 가짜 URL 대신 503으로 명확히 실패시킨다. */
public class UnavailableInquiryAttachmentStorage implements InquiryAttachmentStorage {

    @Override
    public UploadTarget issueUploadUrl(String objectName, String contentType, long contentLength) {
        throw new BusinessException(ErrorCode.IMAGE_UPLOAD_UNAVAILABLE);
    }

    @Override
    public String issueReadUrl(String objectName) {
        throw new BusinessException(ErrorCode.IMAGE_UPLOAD_UNAVAILABLE);
    }
}
