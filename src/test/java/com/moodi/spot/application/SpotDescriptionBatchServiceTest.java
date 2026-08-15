package com.moodi.spot.application;

import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotDescriptionRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotDescriptionBatchServiceTest {

    @Mock
    private SpotDescriptionRepository descriptionRepository;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotTranslationRepository translationRepository;

    @Mock
    private SpotDescriptionClient descriptionClient;

    @Mock
    private SpotDescriptionWriter descriptionWriter;

    private SpotDescriptionBatchService batchService;

    @BeforeEach
    void setUp() {
        batchService = new SpotDescriptionBatchService(
                descriptionRepository, spotRepository, translationRepository,
                descriptionClient, descriptionWriter, 3
        );
    }

    @Test
    @DisplayName("EN description이 없는 스팟에 대해 설명을 생성한다")
    void generate_all_creates_descriptions_for_spots_without_en() {
        // given
        Spot spot = createSpot(1L);
        SpotTranslation translation = SpotTranslation.create(1L, "ko", "가회동성당",
                "종로구에 위치한 성당", "서울시 종로구", null);

        when(descriptionRepository.findSpotIdsWithoutDescription("en-US"))
                .thenReturn(List.of(1L));
        when(spotRepository.findByIdIn(List.of(1L))).thenReturn(List.of(spot));
        when(translationRepository.findBySpotIdAndLocale(1L, "ko"))
                .thenReturn(Optional.of(translation));
        when(descriptionClient.generate(
                eq("가회동성당"), eq("TOURIST_ATTRACTION"), eq("서울"),
                eq(""), eq("종로구에 위치한 성당"), eq("en-US")))
                .thenReturn("A historic cathedral in Jongno-gu.");

        // when
        SpotDescriptionBatchService.BatchResult result = batchService.generateAll("en-US", 0);

        // then
        assertThat(result.generated()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        verify(descriptionWriter).saveIfAbsent(1L, "en-US", "A historic cathedral in Jongno-gu.");
    }

    @Test
    @DisplayName("대상이 없으면 LLM을 호출하지 않는다")
    void generate_all_does_nothing_when_no_targets() {
        // given
        when(descriptionRepository.findSpotIdsWithoutDescription("en-US"))
                .thenReturn(List.of());

        // when
        SpotDescriptionBatchService.BatchResult result = batchService.generateAll("en-US", 0);

        // then
        assertThat(result.generated()).isZero();
        assertThat(result.failed()).isZero();
        verify(descriptionClient, never()).generate(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("LLM 호출 실패 시 해당 스팟만 실패 처리한다")
    void generate_all_handles_failure_gracefully() {
        // given
        Spot spot1 = createSpot(1L);
        Spot spot2 = createSpot(2L);
        SpotTranslation translation1 = SpotTranslation.create(1L, "ko", "스팟1", "설명1", "주소1", null);
        SpotTranslation translation2 = SpotTranslation.create(2L, "ko", "스팟2", "설명2", "주소2", null);

        when(descriptionRepository.findSpotIdsWithoutDescription("en-US"))
                .thenReturn(List.of(1L, 2L));
        when(spotRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(spot1, spot2));
        when(translationRepository.findBySpotIdAndLocale(1L, "ko"))
                .thenReturn(Optional.of(translation1));
        when(translationRepository.findBySpotIdAndLocale(2L, "ko"))
                .thenReturn(Optional.of(translation2));
        when(descriptionClient.generate(eq("스팟1"), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("LLM 호출 실패"));
        when(descriptionClient.generate(eq("스팟2"), any(), any(), any(), any(), any()))
                .thenReturn("Description for spot 2");

        // when
        SpotDescriptionBatchService.BatchResult result = batchService.generateAll("en-US", 0);

        // then
        assertThat(result.generated()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    @Test
    @DisplayName("limit을 지정하면 해당 건수만 처리한다")
    void generate_all_respects_limit() {
        // given
        Spot spot = createSpot(1L);
        SpotTranslation translation = SpotTranslation.create(1L, "ko", "스팟1", "설명1", "주소1", null);

        when(descriptionRepository.findSpotIdsWithoutDescription("en-US"))
                .thenReturn(List.of(1L, 2L, 3L));
        when(spotRepository.findByIdIn(List.of(1L))).thenReturn(List.of(spot));
        when(translationRepository.findBySpotIdAndLocale(1L, "ko"))
                .thenReturn(Optional.of(translation));
        when(descriptionClient.generate(any(), any(), any(), any(), any(), any()))
                .thenReturn("Generated description");

        // when
        SpotDescriptionBatchService.BatchResult result = batchService.generateAll("en-US", 1);

        // then
        assertThat(result.generated()).isEqualTo(1);
    }

    private Spot createSpot(Long id) {
        Spot spot = Spot.create("content-" + id, SpotContentType.TOURIST_ATTRACTION,
                "서울", "종로구", null, "korservice", null, null, null, null, null, null, null);
        try {
            var field = spot.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(spot, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return spot;
    }
}
