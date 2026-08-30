package com.moodi.discovery.application;

import com.moodi.discovery.domain.PickImage;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickImageUploadServiceTest {

    @Mock
    private ImageStorageClient imageStorageClient;

    @InjectMocks
    private PickImageUploadService pickImageUploadService;

    private final UUID memberId = UUID.randomUUID();

    @Test
    @DisplayName("서명 URL 발급 성공")
    void issue_upload_url_success() {
        when(imageStorageClient.issueUploadUrl(anyString(), anyString(), anyLong()))
                .thenReturn(new ImageStorageClient.UploadTarget("https://signed", "picks/key.jpg", 300));

        ImageStorageClient.UploadTarget target =
                pickImageUploadService.issueUploadUrl(memberId, "image/jpeg", 1024);

        assertThat(target.uploadUrl()).isEqualTo("https://signed");
        assertThat(target.imageKey()).isEqualTo("picks/key.jpg");
    }

    @Test
    @DisplayName("발급 요청은 회원별 경로와 정규화된 형식으로 스토리지에 전달된다")
    void issue_upload_url_passes_normalized_values() {
        when(imageStorageClient.issueUploadUrl(anyString(), anyString(), anyLong()))
                .thenReturn(new ImageStorageClient.UploadTarget("https://signed", "key", 300));

        pickImageUploadService.issueUploadUrl(memberId, "IMAGE/JPEG", 2048);

        ArgumentCaptor<String> objectName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentType = ArgumentCaptor.forClass(String.class);
        verify(imageStorageClient).issueUploadUrl(objectName.capture(), contentType.capture(), anyLong());
        assertThat(objectName.getValue()).startsWith("picks/" + memberId + "/").endsWith(".jpg");
        assertThat(contentType.getValue()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("지원하지 않는 형식이면 스토리지를 호출하지 않는다")
    void issue_upload_url_rejects_unsupported_type_before_calling_storage() {
        assertThatThrownBy(() -> pickImageUploadService.issueUploadUrl(memberId, "image/gif", 1024))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICK_UNSUPPORTED_IMAGE_TYPE);

        verify(imageStorageClient, never()).issueUploadUrl(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("용량이 상한을 넘으면 스토리지를 호출하지 않는다")
    void issue_upload_url_rejects_large_image_before_calling_storage() {
        assertThatThrownBy(() ->
                pickImageUploadService.issueUploadUrl(memberId, "image/jpeg", PickImage.MAX_BYTES + 1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICK_IMAGE_TOO_LARGE);

        verify(imageStorageClient, never()).issueUploadUrl(anyString(), anyString(), anyLong());
    }
}
