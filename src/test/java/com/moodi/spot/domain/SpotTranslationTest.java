package com.moodi.spot.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpotTranslationTest {

    @Test
    @DisplayName("SpotTranslation 생성 시 모든 필드가 정상 세팅된다")
    void create_sets_all_fields() {
        SpotTranslation translation = SpotTranslation.create(1L, "ko-KR", "가회동성당",
                "가회동성당은 종로구에 위치한 성당이다.", "서울특별시 종로구 북촌로 57", "가회동");

        assertThat(translation.getSpotId()).isEqualTo(1L);
        assertThat(translation.getLocale()).isEqualTo("ko-KR");
        assertThat(translation.getTitle()).isEqualTo("가회동성당");
        assertThat(translation.getOverview()).isEqualTo("가회동성당은 종로구에 위치한 성당이다.");
        assertThat(translation.getAddr1()).isEqualTo("서울특별시 종로구 북촌로 57");
        assertThat(translation.getAddr2()).isEqualTo("가회동");
    }

    @Test
    @DisplayName("overview와 addr2는 null이 허용된다")
    void create_allows_null_optional_fields() {
        SpotTranslation translation = SpotTranslation.create(1L, "ko-KR", "가회동성당",
                null, "서울특별시 종로구 북촌로 57", null);

        assertThat(translation.getOverview()).isNull();
        assertThat(translation.getAddr2()).isNull();
    }
}
