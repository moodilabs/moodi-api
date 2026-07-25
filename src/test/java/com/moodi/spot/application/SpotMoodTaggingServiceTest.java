package com.moodi.spot.application;

import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.mood.MoodTagRuleEngine;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotStatus;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotMoodTaggingServiceTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotImageRepository spotImageRepository;

    @Mock
    private SpotTranslationRepository spotTranslationRepository;

    @Mock
    private SpotMoodRepository spotMoodRepository;

    @Mock
    private MoodAnalysisClient moodAnalysisClient;

    @Spy
    private MoodTagRuleEngine moodTagRuleEngine = new MoodTagRuleEngine();

    @InjectMocks
    private SpotMoodTaggingService taggingService;

    private static final MoodVector SAMPLE_VECTOR = new MoodVector(
            Map.of(Atmosphere.MOODY, 0.45, Atmosphere.COZY, 0.35, Atmosphere.LIVELY, 0.20),
            Map.of(Color.WARM, 0.70, Color.MONO, 0.20, Color.VIVID, 0.10),
            Map.of(Lighting.OVERCAST, 0.40, Lighting.INDOOR, 0.35, Lighting.DAYLIGHT, 0.25),
            Map.of(Space.ALLEY_LOCAL, 0.80, Space.URBAN, 0.20),
            Map.of(Structure.DENSE, 0.85, Structure.GEOMETRIC, 0.10, Structure.ORGANIC, 0.05),
            Map.of(Era.RETRO, 0.82, Era.TRADITIONAL, 0.10, Era.MODERN, 0.08)
    );

    @Test
    @DisplayName("TAGGING_PENDING 스팟을 태깅하고 PUBLISHED로 전환한다")
    void tag_all_tags_pending_spots_and_publishes() {
        // given
        Spot spot = createSpot(1L);
        when(spotRepository.findByStatus(SpotStatus.TAGGING_PENDING)).thenReturn(List.of(spot));
        when(spotMoodRepository.existsBySpotId(1L)).thenReturn(false);
        when(spotImageRepository.findBySpotId(1L)).thenReturn(
                List.of(SpotImage.createPrimary(1L, "https://example.com/image.jpg")));
        when(spotTranslationRepository.findBySpotIdAndLocale(1L, "ko"))
                .thenReturn(Optional.of(SpotTranslation.create(1L, "ko", "을지로 골목", "레트로 감성 골목", null, null)));
        when(moodAnalysisClient.analyze(anyList(), anyString()))
                .thenReturn(new MoodAnalysisClient.MoodAnalysisResult(SAMPLE_VECTOR, 0.85));
        when(spotMoodRepository.save(any(SpotMood.class))).thenAnswer(inv -> inv.getArgument(0));
        when(spotRepository.save(any(Spot.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        SpotMoodTaggingService.TaggingResult result = taggingService.tagAll();

        // then
        assertThat(result.tagged()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(spot.getStatus()).isEqualTo(SpotStatus.PUBLISHED);
        verify(spotMoodRepository).save(any(SpotMood.class));
    }

    @Test
    @DisplayName("이미 태깅된 스팟은 건너뛴다")
    void tag_all_skips_already_tagged_spot() {
        // given
        Spot spot = createSpot(1L);
        when(spotRepository.findByStatus(SpotStatus.TAGGING_PENDING)).thenReturn(List.of(spot));
        when(spotMoodRepository.existsBySpotId(1L)).thenReturn(true);

        // when
        SpotMoodTaggingService.TaggingResult result = taggingService.tagAll();

        // then
        assertThat(result.tagged()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(moodAnalysisClient, never()).analyze(anyList(), anyString());
    }

    @Test
    @DisplayName("태깅 중 예외 발생 시 해당 스팟만 실패 처리한다")
    void tag_all_handles_failure_gracefully() {
        // given
        Spot spot1 = createSpot(1L);
        Spot spot2 = createSpot(2L);
        when(spotRepository.findByStatus(SpotStatus.TAGGING_PENDING)).thenReturn(List.of(spot1, spot2));
        when(spotMoodRepository.existsBySpotId(any())).thenReturn(false);
        when(spotImageRepository.findBySpotId(1L)).thenThrow(new RuntimeException("DB 오류"));
        when(spotImageRepository.findBySpotId(2L)).thenReturn(List.of());
        when(spotTranslationRepository.findBySpotIdAndLocale(2L, "ko")).thenReturn(Optional.empty());
        when(moodAnalysisClient.analyze(anyList(), anyString()))
                .thenReturn(new MoodAnalysisClient.MoodAnalysisResult(SAMPLE_VECTOR, 0.85));
        when(spotMoodRepository.save(any(SpotMood.class))).thenAnswer(inv -> inv.getArgument(0));
        when(spotRepository.save(any(Spot.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        SpotMoodTaggingService.TaggingResult result = taggingService.tagAll();

        // then
        assertThat(result.tagged()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    private Spot createSpot(Long id) {
        Spot spot = Spot.create("content-" + id, SpotContentType.TOURIST_ATTRACTION,
                "서울", "korservice", null, null, null, null, null, null, null);
        // id 세팅을 위한 리플렉션
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
