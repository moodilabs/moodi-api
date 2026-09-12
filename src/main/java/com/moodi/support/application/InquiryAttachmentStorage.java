package com.moodi.support.application;

/**
 * 문의 첨부를 올릴 대상을 발급하는 포트. Pick 사진({@code discovery.ImageStorageClient})과 같은 방식이지만
 * 개인정보가 섞일 수 있어 버킷을 따로 쓰므로 포트도 분리한다.
 */
public interface InquiryAttachmentStorage {

    UploadTarget issueUploadUrl(String objectName, String contentType, long contentLength);

    String issueReadUrl(String objectName);

    record UploadTarget(String uploadUrl, String attachmentKey, long expiresInSeconds) {}
}
