package com.moodi.support.infrastructure.storage;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.moodi.support.application.InquiryAttachmentStorage;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * GCS V4 서명 URL. 서비스 계정에 `iam.serviceAccounts.signBlob`(Service Account Token Creator)이 필요하다
 * — Pick 버킷과 같은 준비.
 */
public class GcsInquiryAttachmentStorage implements InquiryAttachmentStorage {

    private final Storage storage;
    private final InquiryAttachmentProperties properties;

    public GcsInquiryAttachmentStorage(InquiryAttachmentProperties properties) {
        this(StorageOptions.getDefaultInstance().getService(), properties);
    }

    GcsInquiryAttachmentStorage(Storage storage, InquiryAttachmentProperties properties) {
        this.storage = storage;
        this.properties = properties;
    }

    @Override
    public UploadTarget issueUploadUrl(String objectName, String contentType, long contentLength) {
        BlobInfo blobInfo = BlobInfo.newBuilder(properties.bucket(), objectName)
                .setContentType(contentType)
                .build();
        URL url = sign(
                blobInfo,
                properties.uploadUrlTtlSeconds(),
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withExtHeaders(Map.of("Content-Type", contentType)),
                Storage.SignUrlOption.withV4Signature()
        );
        return new UploadTarget(url.toString(), objectName, properties.uploadUrlTtlSeconds());
    }

    @Override
    public String issueReadUrl(String objectName) {
        BlobInfo blobInfo = BlobInfo.newBuilder(properties.bucket(), objectName).build();
        URL url = sign(
                blobInfo,
                properties.readUrlTtlSeconds(),
                Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                Storage.SignUrlOption.withV4Signature()
        );
        return url.toString();
    }

    /**
     * signBlob 권한(Service Account Token Creator)이 아직 없으면 서명 자체가 실패한다.
     * 500 대신 503으로 돌려 "스토리지 준비 전"과 같은 상태로 보이게 한다 — 클라이언트 분기가 하나면 된다.
     */
    private URL sign(BlobInfo blobInfo, long ttlSeconds, Storage.SignUrlOption... options) {
        try {
            return storage.signUrl(blobInfo, ttlSeconds, TimeUnit.SECONDS, options);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_UNAVAILABLE, e);
        }
    }
}
