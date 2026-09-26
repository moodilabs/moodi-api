package com.moodi.spot.application;

import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.support.SpotFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private SpotMoodTaggingService taggingService;

    private static final Instant FIXED_INSTANT = LocalDateTime.of(2026, 9, 25, 10, 0)
            .atZone(ZoneId.systemDefault()).toInstant();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<Object> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
    }

    private void setupClock() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }

    @Test
    @DisplayName("태깅 대상 스팟을 조회하여 태깅하고 결과를 반환한다")
    void tag_all_tags_pending_spots() {
        // given
        setupClock();
        Spot spot = SpotFixture.createWithId(1L);
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
    void tag_all_handles_exception_failure() {
        // given
        setupClock();
        Spot spot1 = SpotFixture.createWithId(1L);
        Spot spot2 = SpotFixture.createWithId(2L);
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

    @Test
    @DisplayName("태깅 오류 처리 완료(HANDLED_ERROR) 시 실패 카운트에 포함한다")
    void tag_all_counts_handled_error_as_failed() {
        // given
        setupClock();
        Spot spot1 = SpotFixture.createWithId(1L);
        Spot spot2 = SpotFixture.createWithId(2L);
        when(spotRepository.findStaleProcessing(any())).thenReturn(List.of());
        when(spotRepository.findTaggingTargets(any(), anyInt())).thenReturn(List.of(spot1, spot2));
        when(spotMoodTagger.tagSpot(spot1)).thenReturn(SpotMoodTagger.HANDLED_ERROR);
        when(spotMoodTagger.tagSpot(spot2)).thenReturn(100L);

        // when
        SpotMoodTaggingService.TaggingResult result = taggingService.tagAll(0);

        // then
        assertThat(result.tagged()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }
}
