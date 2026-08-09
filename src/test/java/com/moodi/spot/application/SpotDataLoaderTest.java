package com.moodi.spot.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpotDataLoaderTest {

    @Mock
    private SpotBatchWriter spotBatchWriter;

    @Mock
    private SpotRowSaver spotRowSaver;

    @InjectMocks
    private SpotDataLoader spotDataLoader;

    @Test
    @DisplayName("CSV 행을 batch writer로 upsert한다")
    void load_upserts_via_batch_writer() {
        // given
        SpotCsvRow row = createRow("2733967", "관광지", "서울");
        given(spotBatchWriter.upsertAll(anyList()))
                .willReturn(new SpotBatchWriter.UpsertResult(1, 0));

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(List.of(row));

        // then
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.failed()).isZero();
        verify(spotBatchWriter).upsertAll(anyList());
    }

    @Test
    @DisplayName("재적재 시 기존 데이터를 갱신한다")
    void load_updates_existing_spots() {
        // given
        SpotCsvRow row = createRow("2733967", "관광지", "서울");
        given(spotBatchWriter.upsertAll(anyList()))
                .willReturn(new SpotBatchWriter.UpsertResult(0, 1));

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(List.of(row));

        // then
        assertThat(result.inserted()).isZero();
        assertThat(result.updated()).isEqualTo(1);
    }

    @Test
    @DisplayName("파싱 실패 행은 batch에 포함되지 않는다")
    void load_excludes_parse_failed_rows() {
        // given
        SpotCsvRow failRow = createRow("111", "존재하지않는유형", "서울");
        SpotCsvRow successRow = createRow("222", "관광지", "서울");

        given(spotBatchWriter.upsertAll(anyList()))
                .willReturn(new SpotBatchWriter.UpsertResult(1, 0));

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(List.of(failRow, successRow));

        // then
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    @Test
    @DisplayName("batch 실패 시 개별 저장으로 전환하여 실패 행만 격리한다")
    void load_falls_back_to_individual_save_on_batch_failure() {
        // given
        SpotCsvRow row1 = createRow("111", "관광지", "서울");
        SpotCsvRow row2 = createRow("222", "관광지", "서울");

        willThrow(new RuntimeException("DB error"))
                .given(spotBatchWriter).upsertAll(anyList());
        willThrow(new RuntimeException("constraint violation"))
                .given(spotRowSaver).saveRow(row1);

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(List.of(row1, row2));

        // then
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        verify(spotRowSaver).saveRow(row1);
        verify(spotRowSaver).saveRow(row2);
    }

    @Test
    @DisplayName("전체 파싱 실패 시 batch writer를 호출하지 않는다")
    void load_skips_batch_when_all_parse_failed() {
        // given
        SpotCsvRow failRow = createRow("111", "존재하지않는유형", "서울");

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(List.of(failRow));

        // then
        assertThat(result.failed()).isEqualTo(1);
        verify(spotBatchWriter, never()).upsertAll(anyList());
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
                .addr1("서울특별시 종로구 세종대로 175")
                .addr2("")
                .tel("")
                .lclsSystm1("")
                .lclsSystm2("")
                .lclsSystm3("")
                .homepage("")
                .build();
    }
}
