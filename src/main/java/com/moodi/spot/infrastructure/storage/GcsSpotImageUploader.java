package com.moodi.spot.infrastructure.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.moodi.spot.application.SpotImageUploader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@ConditionalOnProperty(name = "gcs.spot-image.enabled", havingValue = "true")
public class GcsSpotImageUploader implements SpotImageUploader {

    private static final String PUBLIC_URL_FORMAT = "https://storage.googleapis.com/%s/%s";

    private final Storage storage;
    private final String bucketName;
    private final HttpClient httpClient;

    public GcsSpotImageUploader(@Value("${gcs.spot-image.bucket}") String bucketName) {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucketName = bucketName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String upload(String sourceUrl, String fileName) {
        byte[] imageBytes = download(sourceUrl);
        String objectName = "spots/" + fileName;
        String contentType = detectContentType(sourceUrl);

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();
        storage.create(blobInfo, imageBytes);

        return String.format(PUBLIC_URL_FORMAT, bucketName, objectName);
    }

    private byte[] download(String sourceUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(sourceUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("이미지 다운로드 실패: HTTP " + response.statusCode());
            }

            try (InputStream is = response.body()) {
                return is.readAllBytes();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("이미지 다운로드 중 인터럽트 발생: " + sourceUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("이미지 다운로드 실패: " + sourceUrl, e);
        }
    }

    private String detectContentType(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png")) {
            return "image/png";
        }
        if (lower.contains(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
