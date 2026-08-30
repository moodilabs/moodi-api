package com.moodi.discovery.application;

import com.moodi.discovery.domain.PickImage;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * DSC-04 사진 업로드용 서명 URL을 발급한다.
 *
 * <p>업로드 자체는 클라이언트가 GCS로 직접 하고, 서버는 "무엇을 어디에 올릴 수 있는지"만 정한다.
 * 파일 정책 검증({@link PickImage})을 발급 전에 끝내는 이유가 여기 있다.
 */
@Service
public class PickImageUploadService {

    private final ImageStorageClient imageStorageClient;

    public PickImageUploadService(ImageStorageClient imageStorageClient) {
        this.imageStorageClient = imageStorageClient;
    }

    public ImageStorageClient.UploadTarget issueUploadUrl(UUID memberId, String contentType, long contentLength) {
        PickImage image = PickImage.of(contentType, contentLength);
        return imageStorageClient.issueUploadUrl(
                image.objectName(memberId),
                image.getType().getContentType(),
                image.getContentLength()
        );
    }
}
