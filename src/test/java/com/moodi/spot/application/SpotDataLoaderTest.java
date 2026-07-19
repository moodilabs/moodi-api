package com.moodi.spot.application;

import com.moodi.spot.domain.SpotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpotDataLoaderTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotRowSaver spotRowSaver;

    @InjectMocks
    private SpotDataLoader spotDataLoader;

    @Test
    @DisplayName("CSV 행을 SpotRowSaver를 통해 저장한다")
    void load_saves_via_row_saver() {
        // given
        SpotCsvRow row = createRow("2733967", "관광지", "서울");
        given(spotRepository.existsBySourceAndContentId("kor_service", "2733967")).willReturn(false);

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(List.of(row));

        // then
        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
        verify(spotRowSaver).save(row);
    }

    @Test
    @DisplayName("이미 존재하는 source+contentId는 건너뛴다")
    void load_skips_existing_spot() {
        // given
        SpotCsvRow row = createRow("2733967", "관광지", "서울");
        given(spotRepository.existsBySourceAndContentId("kor_service", "2733967")).willReturn(true);

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(List.of(row));

        // then
        assertThat(result.saved()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(spotRowSaver, never()).save(any());
    }

    @Test
    @DisplayName("저장 실패 행은 건너뛰고 나머지는 계속 저장한다")
    void load_skips_failed_row_and_continues() {
        // given
        SpotCsvRow failRow = createRow("111", "존재하지않는유형", "서울");
        SpotCsvRow successRow = createRow("222", "관광지", "서울");

        given(spotRepository.existsBySourceAndContentId("kor_service", "111")).willReturn(false);
        given(spotRepository.existsBySourceAndContentId("kor_service", "222")).willReturn(false);
        willThrow(new IllegalArgumentException("Unknown content type label: 존재하지않는유형"))
                .given(spotRowSaver).save(failRow);

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(List.of(failRow, successRow));

        // then
        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        verify(spotRowSaver).save(successRow);
    }

    private SpotCsvRow createRow(String contentId, String contentType, String area) {
        return SpotCsvRow.builder()
                .contentId(contentId)
                .title("테스트")
                .contentType(contentType)
                .area(area)
                .source("kor_service")
                .overview("")
                .spotImage("")
                .longitude("126.98")
                .latitude("37.58")
                .addr1("주소")
                .addr2("")
                .tel("")
                .lclsSystm1("")
                .lclsSystm2("")
                .lclsSystm3("")
                .homepage("")
                .build();
    }
}
