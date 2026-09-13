package com.moodi.discovery.infrastructure.storage;

import com.moodi.discovery.application.ImageStorageClient;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

/**
 * 버킷·IAM이 아직 준비되지 않은 환경에서 쓰는 대체 구현.
 *
 * <p>가짜 URL을 내주면 클라이언트가 업로드에 성공한 줄 알고 다음 단계로 넘어간다.
 * 그래서 성공을 흉내내지 않고 503으로 명확히 실패시킨다.
 */
public class UnavailableImageStorageClient implements ImageStorageClient {

    @Override
    public UploadTarget issueUploadUrl(String objectName, String contentType, long contentLength) {
        throw new BusinessException(ErrorCode.IMAGE_UPLOAD_UNAVAILABLE);
    }

    @Override
    public String issueReadUrl(String objectName) {
        throw new BusinessException(ErrorCode.IMAGE_UPLOAD_UNAVAILABLE);
    }

    /** 스토리지가 없으면 지울 사진도 없다 — 탈퇴를 막지 않는다. */
    @Override
    public void delete(String objectName) {
    }
}
