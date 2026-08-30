package com.moodi.discovery.infrastructure.storage;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.moodi.discovery.application.ImageStorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GcsImageStorageClientTest {

    private static final String BUCKET = "moodi-pick-uploads";
    private static final String OBJECT_NAME = "picks/3f1c/8a2e.jpg";

    private Storage storage;
    private GcsImageStorageClient client;

    @BeforeEach
    void setUp() throws Exception {
        storage = mock(Storage.class);
        client = new GcsImageStorageClient(storage, new PickImageProperties(true, BUCKET, 300, 300));
        when(storage.signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), any(Storage.SignUrlOption[].class)))
                .thenReturn(URI.create("https://signed.example/put").toURL());
    }

    @Test
    @DisplayName("설정한 버킷과 객체 경로로 서명 URL을 발급한다")
    void issue_upload_url_signs_configured_bucket_and_object() {
        ImageStorageClient.UploadTarget target = client.issueUploadUrl(OBJECT_NAME, "image/jpeg", 1024);

        BlobInfo blobInfo = captureBlobInfo();
        assertThat(blobInfo.getBucket()).isEqualTo(BUCKET);
        assertThat(blobInfo.getName()).isEqualTo(OBJECT_NAME);
        assertThat(blobInfo.getContentType()).isEqualTo("image/jpeg");
        assertThat(target.uploadUrl()).isEqualTo("https://signed.example/put");
    }

    @Test
    @DisplayName("업로드 후 돌려줄 키로 공개 URL이 아닌 객체 경로를 내보낸다")
    void issue_upload_url_returns_object_key_not_public_url() {
        ImageStorageClient.UploadTarget target = client.issueUploadUrl(OBJECT_NAME, "image/jpeg", 1024);

        assertThat(target.imageKey()).isEqualTo(OBJECT_NAME);
        assertThat(target.imageKey()).doesNotContain("storage.googleapis.com");
    }

    @Test
    @DisplayName("설정한 유효 시간이 서명과 응답에 함께 적용된다")
    void issue_upload_url_applies_configured_ttl() {
        ImageStorageClient.UploadTarget target = client.issueUploadUrl(OBJECT_NAME, "image/jpeg", 1024);

        verify(storage).signUrl(any(BlobInfo.class), eq(300L), eq(TimeUnit.SECONDS),
                any(Storage.SignUrlOption[].class));
        assertThat(target.expiresInSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("PUT 메서드와 V4 서명, Content-Type 고정 옵션으로 서명한다")
    void issue_upload_url_signs_with_put_and_v4() {
        client.issueUploadUrl(OBJECT_NAME, "image/jpeg", 1024);

        // SignUrlOption은 값 동등성이 없어 내부 option/value를 읽어 비교한다.
        assertThat(captureSignOptions()).containsExactlyInAnyOrder(
                "HTTP_METHOD=PUT",
                "SIGNATURE_VERSION=V4",
                "EXT_HEADERS=" + Map.of("Content-Type", "image/jpeg")
        );
    }

    @Test
    @DisplayName("유효 시간을 설정하지 않으면 기본값 5분을 쓴다")
    void properties_fall_back_to_default_ttl() {
        assertThat(new PickImageProperties(true, BUCKET, 0, 0).uploadUrlTtlSeconds()).isEqualTo(300);
    }

    private BlobInfo captureBlobInfo() {
        ArgumentCaptor<BlobInfo> captor = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage).signUrl(captor.capture(), anyLong(), any(TimeUnit.class),
                any(Storage.SignUrlOption[].class));
        return captor.getValue();
    }

    private List<String> captureSignOptions() {
        ArgumentCaptor<Storage.SignUrlOption[]> captor = ArgumentCaptor.forClass(Storage.SignUrlOption[].class);
        verify(storage).signUrl(any(BlobInfo.class), anyLong(), any(TimeUnit.class), captor.capture());
        return Arrays.stream(captor.getValue())
                .map(option -> ReflectionTestUtils.getField(option, "option")
                        + "=" + ReflectionTestUtils.getField(option, "value"))
                .toList();
    }
}
