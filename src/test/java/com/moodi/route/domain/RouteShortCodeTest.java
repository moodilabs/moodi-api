package com.moodi.route.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RouteShortCodeTest {

    @Test
    @DisplayName("단축 코드는 base62 8자리로 생성된다")
    void generate_base62_eight_chars() {
        // when
        String code = RouteShortCode.generate();

        // then
        assertThat(code).hasSize(RouteShortCode.LENGTH).matches("^[0-9A-Za-z]{8}$");
        assertThat(RouteShortCode.isValid(code)).isTrue();
    }

    @Test
    @DisplayName("연속 생성한 코드는 서로 다르다")
    void generate_is_random() {
        // when
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(RouteShortCode.generate());
        }

        // then
        assertThat(codes).hasSize(1000);
    }

    @Test
    @DisplayName("길이가 다르거나 base62 밖의 문자가 섞이면 코드 형식이 아니다")
    void is_valid_rejects_malformed() {
        assertThat(RouteShortCode.isValid(null)).isFalse();
        assertThat(RouteShortCode.isValid("")).isFalse();
        assertThat(RouteShortCode.isValid("abc1234")).isFalse();
        assertThat(RouteShortCode.isValid("abc123456")).isFalse();
        assertThat(RouteShortCode.isValid("abc-1234")).isFalse();
        assertThat(RouteShortCode.isValid("한글코드1234")).isFalse();
    }
}
