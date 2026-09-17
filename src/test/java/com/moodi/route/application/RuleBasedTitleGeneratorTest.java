package com.moodi.route.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedTitleGeneratorTest {

    private final RuleBasedTitleGenerator generator = new RuleBasedTitleGenerator();

    @Test
    @DisplayName("무드 + 지역 + 기간 제목 생성")
    void generate_title_with_mood_area_and_period() {
        String title = generator.generate(List.of("Seoul"), List.of("retro", "retro", "nature"), 2);

        assertThat(title).isEqualTo("Retro Seoul 2-Day Trip");
    }

    @Test
    @DisplayName("무드 없으면 무드 생략")
    void generate_title_no_mood() {
        String title = generator.generate(List.of("Seoul"), List.of(), 2);

        assertThat(title).isEqualTo("Seoul 2-Day Trip");
    }

    @Test
    @DisplayName("지역 없으면 지역 생략")
    void generate_title_no_area() {
        String title = generator.generate(List.of(), List.of("serene"), 3);

        assertThat(title).isEqualTo("Serene 3-Day Trip");
    }

    @Test
    @DisplayName("무드·지역 둘 다 없으면 기간만")
    void generate_title_period_only() {
        String title = generator.generate(List.of(), List.of(), 1);

        assertThat(title).isEqualTo("1-Day Trip");
    }

    @Test
    @DisplayName("복수 지역이면 첫 번째 지역 사용")
    void generate_title_multiple_areas() {
        String title = generator.generate(List.of("Seoul", "Busan"), List.of("lively"), 4);

        assertThat(title).isEqualTo("Lively Seoul 4-Day Trip");
    }

    @Test
    @DisplayName("40자 초과 시 말줄임")
    void generate_title_truncated_at_40_chars() {
        String title = generator.generate(
                List.of("Gyeongsangbuk-do"),
                List.of("golden_hour"),
                5);

        assertThat(title.length()).isLessThanOrEqualTo(40);
    }
}
