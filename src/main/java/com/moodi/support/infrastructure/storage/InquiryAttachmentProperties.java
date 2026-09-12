package com.moodi.support.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 문의 첨부 버킷. 개인 사진·개인정보가 섞일 수 있어 Pick·스팟 버킷과 분리한다. */
@ConfigurationProperties("gcs.inquiry-upload")
public record InquiryAttachmentProperties(boolean enabled, String bucket, long uploadUrlTtlSeconds,
                                          long readUrlTtlSeconds) {

    private static final long DEFAULT_TTL_SECONDS = 300;

    public InquiryAttachmentProperties {
        if (uploadUrlTtlSeconds <= 0) {
            uploadUrlTtlSeconds = DEFAULT_TTL_SECONDS;
        }
        if (readUrlTtlSeconds <= 0) {
            readUrlTtlSeconds = DEFAULT_TTL_SECONDS;
        }
    }
}
