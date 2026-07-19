package com.moodi.spot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpotImageTest {

    @Test
    @DisplayName("대표 이미지 생성 시 isPrimary가 true이고 sortOrder가 0이다")
    void create_primary_sets_primary_and_sort_order() {
        SpotImage image = SpotImage.createPrimary(1L, "https://example.com/image.jpg");

        assertThat(image.getSpotId()).isEqualTo(1L);
        assertThat(image.getImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(image.isPrimary()).isTrue();
        assertThat(image.getSortOrder()).isZero();
    }
}
