package com.moodi.route.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedTitleGeneratorTest {

    private final RuleBasedTitleGenerator generator = new RuleBasedTitleGenerator();

    @Test
    @DisplayName("지역 + 1박 2일 제목 생성")
    void generate_title_with_area_and_period() {
        String title = generator.generate(List.of("성수"), 2);

        assertThat(title).isEqualTo("성수 1박 2일 코스");
    }

    @Test
    @DisplayName("당일치기 제목 생성")
    void generate_title_single_day() {
        String title = generator.generate(List.of("부산"), 1);

        assertThat(title).isEqualTo("부산 당일치기 코스");
    }

    @Test
    @DisplayName("지역 없으면 '여행' 사용")
    void generate_title_no_area() {
        String title = generator.generate(List.of(), 3);

        assertThat(title).isEqualTo("여행 2박 3일 코스");
    }

    @Test
    @DisplayName("복수 지역이면 첫 번째 지역 사용")
    void generate_title_multiple_areas() {
        String title = generator.generate(List.of("서울", "부산", "제주"), 4);

        assertThat(title).isEqualTo("서울 3박 4일 코스");
    }
}
