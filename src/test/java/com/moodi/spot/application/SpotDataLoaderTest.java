package com.moodi.spot.application;

import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
import com.moodi.spot.support.SpotFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpotDataLoaderTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotTranslationRepository spotTranslationRepository;

    @Mock
    private SpotImageRepository spotImageRepository;

    @InjectMocks
    private SpotDataLoader spotDataLoader;

    @Test
    @DisplayName("CSV 행을 Spot, SpotTranslation, SpotImage로 저장한다")
    void load_saves_spot_translation_and_image() {
        // given
        SpotCsvRow row = SpotCsvRow.builder()
                .contentId("2733967")
                .title("가회동성당")
                .contentType("관광지")
                .area("서울")
                .source("kor_service")
                .overview("가회동성당 개요")
                .spotImage("https://example.com/image.jpg")
                .longitude("126.98")
                .latitude("37.58")
                .addr1("서울특별시 종로구 북촌로 57")
                .addr2("")
                .tel("")
                .lclsSystm1("HS")
                .lclsSystm2("HS03")
                .lclsSystm3("HS030200")
                .build();

        given(spotRepository.existsBySourceAndContentId("kor_service", "2733967")).willReturn(false);
        given(spotRepository.save(any(Spot.class))).willReturn(SpotFixture.createWithId(1L));

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(List.of(row));

        // then
        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
        verify(spotRepository).save(any(Spot.class));
        verify(spotTranslationRepository).save(any(SpotTranslation.class));
        verify(spotImageRepository).save(any(SpotImage.class));
    }

    @Test
    @DisplayName("이미지가 비어있으면 SpotImage를 저장하지 않는다")
    void load_skips_image_when_blank() {
        // given
        SpotCsvRow row = SpotCsvRow.builder()
                .contentId("123")
                .title("테스트")
                .contentType("관광지")
                .area("서울")
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
                .build();

        given(spotRepository.existsBySourceAndContentId("kor_service", "123")).willReturn(false);
        given(spotRepository.save(any(Spot.class))).willReturn(SpotFixture.createWithId(1L));

        // when
        spotDataLoader.load(List.of(row));

        // then
        verify(spotImageRepository, never()).save(any(SpotImage.class));
    }

    @Test
    @DisplayName("이미 존재하는 source+contentId는 건너뛴다")
    void load_skips_existing_spot() {
        // given
        SpotCsvRow row = SpotCsvRow.builder()
                .contentId("2733967")
                .title("가회동성당")
                .contentType("관광지")
                .area("서울")
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
                .build();

        given(spotRepository.existsBySourceAndContentId("kor_service", "2733967")).willReturn(true);

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(List.of(row));

        // then
        assertThat(result.saved()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(spotRepository, never()).save(any(Spot.class));
    }

    @Test
    @DisplayName("숙박 유형은 routeExcluded가 true로 저장된다")
    void load_sets_route_excluded_for_accommodation() {
        // given
        SpotCsvRow row = SpotCsvRow.builder()
                .contentId("999")
                .title("호텔")
                .contentType("숙박")
                .area("부산")
                .source("kor_service")
                .overview("")
                .spotImage("")
                .longitude("129.05")
                .latitude("35.15")
                .addr1("부산 주소")
                .addr2("")
                .tel("")
                .lclsSystm1("")
                .lclsSystm2("")
                .lclsSystm3("")
                .build();

        given(spotRepository.existsBySourceAndContentId("kor_service", "999")).willReturn(false);
        given(spotRepository.save(any(Spot.class))).willReturn(
                SpotFixture.createWithId(1L, "999", "kor_service"));

        // when
        spotDataLoader.load(List.of(row));

        // then
        verify(spotRepository).save(argThat(spot ->
                spot.getContentType() == SpotContentType.ACCOMMODATION && spot.isRouteExcluded()));
    }

    @Test
    @DisplayName("잘못된 content_type이면 해당 행만 실패하고 나머지는 저장된다")
    void load_skips_row_with_invalid_content_type() {
        // given
        SpotCsvRow validRow = SpotCsvRow.builder()
                .contentId("111")
                .title("정상")
                .contentType("관광지")
                .area("서울")
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
                .build();

        SpotCsvRow invalidRow = SpotCsvRow.builder()
                .contentId("222")
                .title("오류")
                .contentType("존재하지않는유형")
                .area("서울")
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
                .build();

        given(spotRepository.existsBySourceAndContentId("kor_service", "111")).willReturn(false);
        given(spotRepository.existsBySourceAndContentId("kor_service", "222")).willReturn(false);
        given(spotRepository.save(any(Spot.class))).willReturn(SpotFixture.createWithId(1L));

        // when
        SpotDataLoader.LoadResult result = spotDataLoader.load(List.of(validRow, invalidRow));

        // then
        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }
}
