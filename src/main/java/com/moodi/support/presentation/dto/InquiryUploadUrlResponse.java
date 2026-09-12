package com.moodi.support.presentation.dto;

import com.moodi.support.application.InquiryAttachmentStorage;

public record InquiryUploadUrlResponse(String uploadUrl, String attachmentKey, long expiresInSeconds) {

    public static InquiryUploadUrlResponse from(InquiryAttachmentStorage.UploadTarget target) {
        return new InquiryUploadUrlResponse(target.uploadUrl(), target.attachmentKey(), target.expiresInSeconds());
    }
}
