package com.moodi.discovery.infrastructure.storage;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.moodi.discovery.application.ImageStorageClient;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * GCS V4 서명 URL로 업로드 대상을 발급한다.
 *
 * <p>{@code spot}의 {@code GcsSpotImageUploader}와 방향이 반대다. 그쪽은 서버가 외부 URL을 내려받아
 * 올리는 배치용이고, 여기는 클라이언트가 자기 기기의 사진을 직접 올린다. 그래서 재사용하지 않는다.
 *
 * <p>서명에는 서비스 계정의 {@code iam.serviceAccounts.signBlob} 권한이 필요하다.
 * Cloud Run 기본 서비스 계정에 Service Account Token Creator를 부여해야 키 파일 없이 동작한다.
 */
public class GcsImageStorageClient implements ImageStorageClient {

    private final Storage storage;
    private final PickImageProperties properties;

    public GcsImageStorageClient(PickImageProperties properties) {
        this(StorageOptions.getDefaultInstance().getService(), properties);
    }

    GcsImageStorageClient(Storage storage, PickImageProperties properties) {
        this.storage = storage;
        this.properties = properties;
    }

    @Override
    public UploadTarget issueUploadUrl(String objectName, String contentType, long contentLength) {
        BlobInfo blobInfo = BlobInfo.newBuilder(properties.bucket(), objectName)
                .setContentType(contentType)
                .build();

        // Content-Type을 서명에 포함하면 발급 시 확정한 형식으로만 업로드할 수 있다.
        URL url = storage.signUrl(
                blobInfo,
                properties.uploadUrlTtlSeconds(),
                TimeUnit.SECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withExtHeaders(Map.of("Content-Type", contentType)),
                Storage.SignUrlOption.withV4Signature()
        );

        return new UploadTarget(url.toString(), objectName, properties.uploadUrlTtlSeconds());
    }
}
