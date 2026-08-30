package com.moodi.discovery.infrastructure.storage;

import com.moodi.discovery.application.ImageStorageClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageStorageConfig {

    @Bean
    @ConditionalOnProperty(name = "gcs.pick-image.enabled", havingValue = "true")
    public ImageStorageClient gcsImageStorageClient(PickImageProperties properties) {
        return new GcsImageStorageClient(properties);
    }

    @Bean
    @ConditionalOnProperty(name = "gcs.pick-image.enabled", havingValue = "false", matchIfMissing = true)
    public ImageStorageClient unavailableImageStorageClient() {
        return new UnavailableImageStorageClient();
    }
}
