package com.moodi.discovery.infrastructure.storage;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnavailableImageStorageClientTest {

    private final UnavailableImageStorageClient client = new UnavailableImageStorageClient();

    @Test
    @DisplayName("버킷이 준비되지 않은 환경에서는 가짜 URL 대신 503으로 실패한다")
    void issue_upload_url_fails_fast() {
        assertThatThrownBy(() -> client.issueUploadUrl("picks/a.jpg", "image/jpeg", 1024))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_UNAVAILABLE);
    }
}
