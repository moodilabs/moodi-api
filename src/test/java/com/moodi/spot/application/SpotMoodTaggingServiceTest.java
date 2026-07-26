package com.moodi.spot.application;

import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotMoodTaggingServiceTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotMoodRepository spotMoodRepository;

    @Mock
    private SpotMoodTagger spotMoodTagger;

    @InjectMocks
    private SpotMoodTaggingService taggingService;

    @Test
    @DisplayName("TAGGING_PENDING 스팟을 태깅하고 결과를 반환한다")
    void tag_all_tags_pending_spots_and_publishes() {
        // given
        Spot spot = createSpot(1L);
        when(spotRepository.findByStatus(SpotStatus.TAGGING_PENDING)).thenReturn(List.of(spot));
        when(spotMoodRepository.existsBySpotId(1L)).thenReturn(false);
        doNothing().when(spotMoodTagger).tagSpot(spot);

        // when
        SpotMoodTaggingService.TaggingResult result = taggingService.tagAll(0);

        // then
        assertThat(result.tagged()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(result.invalidResponses()).isZero();
        verify(spotMoodTagger).tagSpot(spot);
    }

    @Test
    @DisplayName("이미 태깅된 스팟은 건너뛴다")
    void tag_all_skips_already_tagged_spot() {
        // given
        Spot spot = createSpot(1L);
        when(spotRepository.findByStatus(SpotStatus.TAGGING_PENDING)).thenReturn(List.of(spot));
        when(spotMoodRepository.existsBySpotId(1L)).thenReturn(true);

        // when
        SpotMoodTaggingService.TaggingResult result = taggingService.tagAll(0);

        // then
        assertThat(result.tagged()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(spotMoodTagger, never()).tagSpot(any());
    }

    @Test
    @DisplayName("태깅 중 예외 발생 시 해당 스팟만 실패 처리한다")
    void tag_all_handles_failure_gracefully() {
        // given
        Spot spot1 = createSpot(1L);
        Spot spot2 = createSpot(2L);
        when(spotRepository.findByStatus(SpotStatus.TAGGING_PENDING)).thenReturn(List.of(spot1, spot2));
        when(spotMoodRepository.existsBySpotId(any())).thenReturn(false);
        doThrow(new RuntimeException("DB 오류")).when(spotMoodTagger).tagSpot(spot1);
        doNothing().when(spotMoodTagger).tagSpot(spot2);

        // when
        SpotMoodTaggingService.TaggingResult result = taggingService.tagAll(0);

        // then
        assertThat(result.tagged()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    private Spot createSpot(Long id) {
        Spot spot = Spot.create("content-" + id, SpotContentType.TOURIST_ATTRACTION,
                "서울", "korservice", null, null, null, null, null, null, null);
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
