package com.moodi.discovery.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PickImageTest {

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/heic"})
    @DisplayName("JPG·PNG·HEIC는 업로드할 수 있다")
    void of_allows_supported_types(String contentType) {
        PickImage image = PickImage.of(contentType, 1024);

        assertThat(image.getType().getContentType()).isEqualTo(contentType);
    }

    @Test
    @DisplayName("대소문자와 앞뒤 공백이 섞여도 형식을 인식한다")
    void of_normalizes_content_type() {
        PickImage image = PickImage.of("  IMAGE/JPEG ", 1024);

        assertThat(image.getType()).isEqualTo(PickImageType.JPEG);
    }

    @Test
    @DisplayName("스팟 이미지에서 쓰는 webp는 Pick에서는 허용하지 않는다")
    void of_rejects_webp() {
        assertThatThrownBy(() -> PickImage.of("image/webp", 1024))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICK_UNSUPPORTED_IMAGE_TYPE);
    }

    @Test
    @DisplayName("형식이 비어 있으면 업로드할 수 없다")
    void of_rejects_blank_content_type() {
        assertThatThrownBy(() -> PickImage.of("  ", 1024))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICK_UNSUPPORTED_IMAGE_TYPE);
    }

    @Test
    @DisplayName("10MB까지는 업로드할 수 있다")
    void of_allows_max_bytes() {
        PickImage image = PickImage.of("image/jpeg", PickImage.MAX_BYTES);

        assertThat(image.getContentLength()).isEqualTo(PickImage.MAX_BYTES);
    }

    @Test
    @DisplayName("10MB를 넘으면 업로드할 수 없다")
    void of_rejects_over_max_bytes() {
        assertThatThrownBy(() -> PickImage.of("image/jpeg", PickImage.MAX_BYTES + 1))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICK_IMAGE_TOO_LARGE);
    }

    @Test
    @DisplayName("용량이 0 이하면 업로드할 수 없다")
    void of_rejects_non_positive_length() {
        assertThatThrownBy(() -> PickImage.of("image/jpeg", 0))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICK_IMAGE_TOO_LARGE);
    }

    @Test
    @DisplayName("객체 경로는 회원별로 나뉘고 형식에 맞는 확장자를 갖는다")
    void object_name_is_scoped_by_member() {
        UUID memberId = UUID.randomUUID();

        String objectName = PickImage.of("image/heic", 1024).objectName(memberId);

        assertThat(objectName).startsWith("picks/" + memberId + "/").endsWith(".heic");
    }

    @Test
    @DisplayName("객체 경로는 매번 달라 같은 회원의 사진끼리 덮어쓰지 않는다")
    void object_name_is_unique_per_call() {
        UUID memberId = UUID.randomUUID();
        PickImage image = PickImage.of("image/jpeg", 1024);

        assertThat(image.objectName(memberId)).isNotEqualTo(image.objectName(memberId));
    }
}
