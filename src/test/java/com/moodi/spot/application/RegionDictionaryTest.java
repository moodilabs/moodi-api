package com.moodi.spot.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegionDictionaryTest {

    @Test
    @DisplayName("한국어 지역명을 영문으로 바꾼다")
    void translate_to_english() {
        assertThat(RegionDictionary.translateArea("서울")).isEqualTo("Seoul");
        assertThat(RegionDictionary.translateDistrict("성동구")).isEqualTo("Seongdong-gu");
    }

    @Test
    @DisplayName("영문 지역명을 한국어로 되돌린다")
    void translate_back_to_korean() {
        assertThat(RegionDictionary.toKoreanArea("Seoul")).isEqualTo("서울");
        assertThat(RegionDictionary.toKoreanDistrict("Seongdong-gu")).isEqualTo("성동구");
    }

    @Test
    @DisplayName("모든 항목이 왕복 변환된다")
    void translation_round_trips() {
        for (String area : new String[]{"서울", "부산", "제주", "강원", "경남"}) {
            assertThat(RegionDictionary.toKoreanArea(RegionDictionary.translateArea(area))).isEqualTo(area);
        }
        for (String district : new String[]{"성동구", "해운대구", "제주시", "중구", "남구"}) {
            assertThat(RegionDictionary.toKoreanDistrict(RegionDictionary.translateDistrict(district)))
                    .isEqualTo(district);
        }
    }

    @Test
    @DisplayName("사전에 없는 값은 그대로 둔다")
    void unknown_value_passes_through() {
        assertThat(RegionDictionary.translateArea("없는지역")).isEqualTo("없는지역");
        assertThat(RegionDictionary.toKoreanArea("Nowhere")).isEqualTo("Nowhere");
    }

    @Test
    @DisplayName("null은 null로 둔다")
    void null_stays_null() {
        assertThat(RegionDictionary.translateArea(null)).isNull();
        assertThat(RegionDictionary.toKoreanArea(null)).isNull();
        assertThat(RegionDictionary.toKoreanDistrict(null)).isNull();
    }
}
