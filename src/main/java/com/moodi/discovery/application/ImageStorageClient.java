package com.moodi.discovery.application;

/**
 * 사용자 사진을 올릴 대상을 발급하는 포트. 어떤 스토리지인지는 알지 못한다.
 *
 * <p>Cloud Run은 요청 크기·타임아웃 제약이 있어 서버가 파일 본문을 받지 않는다.
 * 클라이언트가 발급받은 URL로 직접 PUT한다.
 */
public interface ImageStorageClient {

    UploadTarget issueUploadUrl(String objectName, String contentType, long contentLength);

    /**
     * @param uploadUrl  클라이언트가 PUT할 서명 URL
     * @param imageKey   업로드 후 서버에 돌려줄 객체 키. 비공개 버킷이라 그대로는 열람할 수 없다
     * @param expiresInSeconds 서명 URL 유효 시간
     */
    record UploadTarget(String uploadUrl, String imageKey, long expiresInSeconds) {}
}
