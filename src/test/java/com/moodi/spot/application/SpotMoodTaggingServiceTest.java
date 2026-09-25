package com.moodi.spot.application;

import com.moodi.spot.domain.MoodTaggingStatus;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotMoodTaggingServiceTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotMoodTagger spotMoodTagger;

    @Mock
    private MoodAnalysisClient moodAnalysisClient;

    @Mock
    private Clock clock;

    @InjectMocks
    private SpotMoodTaggingService taggingService;

    private static final Instant FIXED_INSTANT = LocalDateTime.of(2026, 9, 25, 10, 0)
            .atZone(ZoneId.systemDefault()).toInstant();

    private void setupClock() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    @DisplayName("태깅 대상 스팟을 조회하여 태깅하고 결과를 반환한다")
    void tag_all_tags_pending_spots() {
        // given
        setupClock();
        Spot spot = createSpot(1L);
        when(spotRepository.findStaleProcessing(any())).thenReturn(List.of());
        when(spotRepository.findTaggingTargets(any(), anyInt())).thenReturn(List.of(spot));
        when(spotMoodTagger.tagSpot(spot)).thenReturn(100L);

        // when
        SpotMoodTaggingService.TaggingResult result = taggingService.tagAll(0);

        // then
        assertThat(result.tagged()).isEqualTo(1);
        assertThat(result.failed()).isZero();
        verify(spotMoodTagger).tagSpot(spot);
    }

    @Test
    @DisplayName("태깅 대상이 없으면 빈 결과를 반환한다")
    void tag_all_returns_empty_when_no_targets() {
        // given
        setupClock();
        when(spotRepository.findStaleProcessing(any())).thenReturn(List.of());
        when(spotRepository.findTaggingTargets(any(), anyInt())).thenReturn(List.of());

        // when
        SpotMoodTaggingService.TaggingResult result = taggingService.tagAll(0);

        // then
        assertThat(result.tagged()).isZero();
        assertThat(result.failed()).isZero();
        verify(spotMoodTagger, never()).tagSpot(any());
    }

    @Test
    @DisplayName("태깅 중 예외 발생 시 해당 스팟만 실패 처리한다")
    void tag_all_handles_failure_gracefully() {
        // given
        setupClock();
        Spot spot1 = createSpot(1L);
        Spot spot2 = createSpot(2L);
        when(spotRepository.findStaleProcessing(any())).thenReturn(List.of());
        when(spotRepository.findTaggingTargets(any(), anyInt())).thenReturn(List.of(spot1, spot2));
        when(spotMoodTagger.tagSpot(spot1)).thenThrow(new RuntimeException("DB 오류"));
        when(spotMoodTagger.tagSpot(spot2)).thenReturn(100L);

        // when
        SpotMoodTaggingService.TaggingResult result = taggingService.tagAll(0);

        // then
        assertThat(result.tagged()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    private Spot createSpot(Long id) {
        Spot spot = Spot.create("content-" + id, SpotContentType.TOURIST_ATTRACTION,
                "서울", "종로구", null, "korservice", null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(spot, "id", id);
        return spot;
    }
}
