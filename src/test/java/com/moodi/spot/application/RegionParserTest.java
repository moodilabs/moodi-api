package com.moodi.spot.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegionParserTest {

    @Test
    @DisplayName("서울 주소에서 구와 동을 추출한다")
    void parse_seoul_district_and_neighborhood() {
        ParsedRegion result = RegionParser.parse("서울특별시 종로구 북촌로 57 (가회동)");

        assertThat(result.district()).isEqualTo("종로구");
        assertThat(result.neighborhood()).isEqualTo("가회동");
    }

    @Test
    @DisplayName("부산 주소에서 구와 동을 추출한다")
    void parse_busan_district_and_neighborhood() {
        ParsedRegion result = RegionParser.parse("부산광역시 동구 중앙대로 206 (초량동)");

        assertThat(result.district()).isEqualTo("동구");
        assertThat(result.neighborhood()).isEqualTo("초량동");
    }

    @Test
    @DisplayName("군 단위 주소에서 군과 읍을 추출한다")
    void parse_gun_and_eup() {
        ParsedRegion result = RegionParser.parse("부산광역시 기장군 기장읍 기장해안로 (기장읍)");

        assertThat(result.district()).isEqualTo("기장군");
        assertThat(result.neighborhood()).isEqualTo("기장읍");
    }

    @Test
    @DisplayName("괄호가 없는 주소에서 구만 추출하고 동은 null이다")
    void parse_no_parentheses_returns_null_neighborhood() {
        ParsedRegion result = RegionParser.parse("서울특별시 중구 세종대로 110");

        assertThat(result.district()).isEqualTo("중구");
        assertThat(result.neighborhood()).isNull();
    }

    @Test
    @DisplayName("null 입력 시 모두 null을 반환한다")
    void parse_null_returns_nulls() {
        ParsedRegion result = RegionParser.parse(null);

        assertThat(result.district()).isNull();
        assertThat(result.neighborhood()).isNull();
    }

    @Test
    @DisplayName("빈 문자열 입력 시 모두 null을 반환한다")
    void parse_blank_returns_nulls() {
        ParsedRegion result = RegionParser.parse("   ");

        assertThat(result.district()).isNull();
        assertThat(result.neighborhood()).isNull();
    }

    @Test
    @DisplayName("두 번째 토큰이 구/군이 아니면 district는 null이다")
    void parse_non_district_second_token_returns_null() {
        ParsedRegion result = RegionParser.parse("제주특별자치도 서귀포시 중문동");

        assertThat(result.district()).isNull();
        assertThat(result.neighborhood()).isNull();
    }

    @Test
    @DisplayName("괄호 안에 가로 끝나는 동 이름을 추출한다")
    void parse_ga_neighborhood() {
        ParsedRegion result = RegionParser.parse("서울특별시 종로구 종로 99 (종로2가)");

        assertThat(result.district()).isEqualTo("종로구");
        assertThat(result.neighborhood()).isEqualTo("종로2가");
    }
}
