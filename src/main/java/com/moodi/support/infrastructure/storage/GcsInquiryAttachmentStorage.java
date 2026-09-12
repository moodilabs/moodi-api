package com.moodi.support.infrastructure.storage;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.moodi.support.application.InquiryAttachmentStorage;

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

    @Override
    public String issueReadUrl(String objectName) {
        BlobInfo blobInfo = BlobInfo.newBuilder(properties.bucket(), objectName).build();
        URL url = storage.signUrl(
                blobInfo,
                properties.readUrlTtlSeconds(),
                TimeUnit.SECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                Storage.SignUrlOption.withV4Signature()
        );
        return url.toString();
    }
}
