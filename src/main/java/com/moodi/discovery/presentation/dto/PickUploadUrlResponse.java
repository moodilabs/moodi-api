package com.moodi.discovery.presentation.dto;

import com.moodi.discovery.application.ImageStorageClient;

public record PickUploadUrlResponse(
        String uploadUrl,
        String imageKey,
        long expiresInSeconds
) {

    public static PickUploadUrlResponse from(ImageStorageClient.UploadTarget target) {
        return new PickUploadUrlResponse(target.uploadUrl(), target.imageKey(), target.expiresInSeconds());
    }
}
