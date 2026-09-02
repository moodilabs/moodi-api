package com.moodi.discovery.infrastructure.region;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 지명으로 검증한다. 표기법을 규칙 단위로 쪼개 확인해도 지명에서 어떻게 맞물리는지는 드러나지 않는다.
 */
class RomanizerTest {

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("행정 단위 접미사를 떼고 로마자로 옮긴다")
    @CsvSource({
            "성수동, Seongsu",
            "서초동, Seocho",
            "연남동, Yeonnam",
            "청담동, Cheongdam",
            "한남동, Hannam",
            "여의도동, Yeouido",
            "금호동, Geumho"
    })
    void romanize_strips_admin_suffix(String korean, String expected) {
        assertThat(Romanizer.romanizePlaceName(korean)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("자음동화를 표기에 반영한다")
    @CsvSource({
            "종로, Jongno",
            "왕십리, Wangsimni",
            "광안리, Gwangalli",
            "독립문, Dongnimmun",
            "전농동, Jeonnong"
    })
    void romanize_reflects_assimilation(String korean, String expected) {
        assertThat(Romanizer.romanizePlaceName(korean)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("받침 없는 음절 뒤의 ㄹ은 r로 적는다")
    @CsvSource({
            "을지로, Euljiro",
            "이태원, Itaewon",
            "해운대, Haeundae"
    })
    void romanize_rieul_after_open_syllable(String korean, String expected) {
        assertThat(Romanizer.romanizePlaceName(korean)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("두 글자 이름은 접미사를 떼지 않는다 - 한 글자만 남으면 어디인지 알 수 없다")
    @CsvSource({
            "명동, Myeongdong",
            "우동, Udong"
    })
    void romanize_keeps_two_character_name(String korean, String expected) {
        assertThat(Romanizer.romanizePlaceName(korean)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("리·가는 이름의 일부로 굳은 지명이 많아 떼지 않는다")
    @CsvSource({
            "왕십리, Wangsimni",
            "광안리, Gwangalli"
    })
    void romanize_keeps_ri_and_ga(String korean, String expected) {
        assertThat(Romanizer.romanizePlaceName(korean)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("여행자에게 익숙한 지명도 통용 표기와 어긋나지 않는다")
    @CsvSource({
            "홍대, Hongdae",
            "강남, Gangnam",
            "신촌, Sinchon",
            "압구정, Apgujeong"
    })
    void romanize_matches_conventional_spelling(String korean, String expected) {
        assertThat(Romanizer.romanizePlaceName(korean)).isEqualTo(expected);
    }

    @Test
    @DisplayName("빈 값은 그대로 돌려준다")
    void romanize_passes_through_blank() {
        assertThat(Romanizer.romanizePlaceName(null)).isNull();
        assertThat(Romanizer.romanizePlaceName("")).isEmpty();
    }

    @Test
    @DisplayName("한글이 아닌 글자는 그대로 남긴다")
    void romanize_keeps_non_hangul() {
        assertThat(Romanizer.romanizePlaceName("을지로3가")).isEqualTo("Euljiro3ga");
    }
}
