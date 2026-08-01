package com.moodi.route.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedTitleGeneratorTest {

    private final RuleBasedTitleGenerator generator = new RuleBasedTitleGenerator();

    @Test
    @DisplayName("지역 + 2일 제목 생성")
    void generate_title_with_area_and_period() {
        String title = generator.generate(List.of("Seoul"), 2);

        assertThat(title).isEqualTo("2-Day Seoul Trip");
    }

    @Test
    @DisplayName("당일치기 제목 생성")
    void generate_title_single_day() {
        String title = generator.generate(List.of("Busan"), 1);

        assertThat(title).isEqualTo("1-Day Busan Trip");
    }

    @Test
    @DisplayName("지역 없으면 지역 생략")
    void generate_title_no_area() {
        String title = generator.generate(List.of(), 3);

        assertThat(title).isEqualTo("3-Day Trip");
    }

    @Test
    @DisplayName("복수 지역이면 첫 번째 지역 사용")
    void generate_title_multiple_areas() {
        String title = generator.generate(List.of("Seoul", "Busan", "Jeju"), 4);

        assertThat(title).isEqualTo("4-Day Seoul Trip");
    }
}
