package com.moodi.spot.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

class SpotCsvReaderTest {

    private final SpotCsvReader reader = new SpotCsvReader();

    @Test
    @DisplayName("CSV 헤더와 데이터 행을 파싱한다")
    void read_parses_csv_rows() throws IOException {
        // given
        String csv = """
                content_id,title,content_type,area,source,overview,spot_image,longitude,latitude,addr1,addr2,tel,lcls_systm1,lcls_systm2,lcls_systm3
                2733967,가회동성당,관광지,서울,kor_service,,https://example.com/img.jpg,126.98,37.58,서울특별시 종로구 북촌로 57,,,HS,HS03,HS030200
                """;

        // when
        SpotCsvReader.ReadResult result = reader.read(new StringReader(csv));

        // then
        assertThat(result.rows()).hasSize(1);
        assertThat(result.failedRows()).isZero();
        SpotCsvRow row = result.rows().get(0);
        assertThat(row.getContentId()).isEqualTo("2733967");
        assertThat(row.getTitle()).isEqualTo("가회동성당");
        assertThat(row.getContentType()).isEqualTo("관광지");
        assertThat(row.getArea()).isEqualTo("서울");
        assertThat(row.getLongitude()).isEqualTo("126.98");
    }

    @Test
    @DisplayName("overview에 쉼표가 포함된 경우 따옴표로 감싸서 파싱한다")
    void read_handles_quoted_fields_with_commas() throws IOException {
        // given
        String csv = """
                content_id,title,content_type,area,source,overview,spot_image,longitude,latitude,addr1,addr2,tel,lcls_systm1,lcls_systm2,lcls_systm3
                123,테스트,관광지,서울,kor_service,"이것은, 쉼표가 포함된 설명",https://example.com/img.jpg,126.98,37.58,주소,,,,,
                """;

        // when
        SpotCsvReader.ReadResult result = reader.read(new StringReader(csv));

        // then
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).getOverview()).isEqualTo("이것은, 쉼표가 포함된 설명");
    }

    @Test
    @DisplayName("빈 CSV는 빈 리스트를 반환한다")
    void read_returns_empty_list_for_header_only() throws IOException {
        // given
        String csv = """
                content_id,title,content_type,area,source,overview,spot_image,longitude,latitude,addr1,addr2,tel,lcls_systm1,lcls_systm2,lcls_systm3
                """;

        // when
        SpotCsvReader.ReadResult result = reader.read(new StringReader(csv));

        // then
        assertThat(result.rows()).isEmpty();
        assertThat(result.failedRows()).isZero();
    }

    @Test
    @DisplayName("필드가 부족한 행은 실패로 기록하고 나머지 행은 정상 파싱한다")
    void read_skips_malformed_row_and_continues() throws IOException {
        // given
        String csv = """
                content_id,title,content_type,area,source,overview,spot_image,longitude,latitude,addr1,addr2,tel,lcls_systm1,lcls_systm2,lcls_systm3
                111,정상행,관광지,서울,kor_service,,https://example.com/img.jpg,126.98,37.58,주소,,,,,
                222,불완전행
                333,정상행2,문화시설,부산,kor_service,,,129.05,35.15,부산주소,,,,,
                """;

        // when
        SpotCsvReader.ReadResult result = reader.read(new StringReader(csv));

        // then
        assertThat(result.rows()).hasSize(2);
        assertThat(result.failedRows()).isEqualTo(1);
        assertThat(result.rows().get(0).getContentId()).isEqualTo("111");
        assertThat(result.rows().get(1).getContentId()).isEqualTo("333");
    }
}
