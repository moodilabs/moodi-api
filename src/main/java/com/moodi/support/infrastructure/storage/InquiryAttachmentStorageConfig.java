package com.moodi.support.infrastructure.storage;

import com.moodi.support.application.InquiryAttachmentStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InquiryAttachmentStorageConfig {

    @Bean
    @ConditionalOnProperty(name = "gcs.inquiry-upload.enabled", havingValue = "true")
    public InquiryAttachmentStorage gcsInquiryAttachmentStorage(InquiryAttachmentProperties properties) {
        return new GcsInquiryAttachmentStorage(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "gcs.inquiry-upload.enabled", havingValue = "false", matchIfMissing = true)
    public InquiryAttachmentStorage unavailableInquiryAttachmentStorage() {
        return new UnavailableInquiryAttachmentStorage();
    }
}
