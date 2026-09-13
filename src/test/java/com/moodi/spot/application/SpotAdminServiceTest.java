package com.moodi.spot.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.response.CursorResponse;
import com.moodi.spot.application.dto.SpotAdminDetail;
import com.moodi.spot.application.dto.SpotAdminFilter;
import com.moodi.spot.application.dto.SpotAdminRow;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotStatus;
import com.moodi.spot.domain.SpotTranslationRepository;
import com.moodi.spot.support.SpotFixture;
import com.moodi.spot.support.SpotMoodFixture;
import com.moodi.spot.support.SpotTranslationFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotAdminServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final Long SPOT_ID = 1L;

    @Mock
    private SpotRepository spotRepository;
    @Mock
    private SpotAdminQueryRepository spotAdminQueryRepository;
    @Mock
    private SpotTranslationRepository spotTranslationRepository;
    @Mock
    private SpotDescriptionRepository spotDescriptionRepository;
    @Mock
    private SpotMoodRepository spotMoodRepository;
    @Mock
    private SpotImageRepository spotImageRepository;
    @Mock
    private BookmarkQueryRepository bookmarkQueryRepository;

    private SpotAdminService spotAdminService;

    @BeforeEach
    void setUp() {
        spotAdminService = new SpotAdminService(spotRepository, spotAdminQueryRepository, spotTranslationRepository,
                spotDescriptionRepository, spotMoodRepository, spotImageRepository, bookmarkQueryRepository,
                FIXED_CLOCK);
    }

    @Test
    @DisplayName("목록은 id 커서로 페이징한다")
    void get_spots_paginates() {
        SpotAdminFilter filter = new SpotAdminFilter("changgo", SpotStatus.PUBLISHED, null);
        when(spotAdminQueryRepository.findAll(eq(filter), isNull(), eq(2))).thenReturn(List.of(row(30L), row(29L)));

        CursorResponse<SpotAdminRow> result = spotAdminService.getSpots(filter, null, 1);

        assertThat(result.items()).extracting(SpotAdminRow::id).containsExactly(30L);
        assertThat(result.nextCursor()).isEqualTo("30");
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("상세는 번역·설명·무드·이미지·북마크 수를 모은다")
    void get_spot_aggregates() {
        Spot spot = SpotFixture.createWithId(SPOT_ID);
        when(spotRepository.findById(SPOT_ID)).thenReturn(Optional.of(spot));
        when(spotTranslationRepository.findBySpotIdAndLocale(SPOT_ID, "en-US"))
                .thenReturn(Optional.of(SpotTranslationFixture.create(SPOT_ID, "en-US", "Daelim Changgo")));
        when(spotDescriptionRepository.findBySpotIdAndLocale(SPOT_ID, "en-US"))
                .thenReturn(Optional.of(SpotDescription.create(SPOT_ID, "en-US", "A 1970s rice warehouse.")));
        when(spotMoodRepository.findBySpotId(SPOT_ID))
                .thenReturn(Optional.of(SpotMoodFixture.create(SPOT_ID, List.of(MoodTag.RETRO, MoodTag.LIVELY))));
        when(spotImageRepository.findBySpotId(SPOT_ID)).thenReturn(List.of());
        when(bookmarkQueryRepository.countBySpotId(SPOT_ID)).thenReturn(1532L);

        SpotAdminDetail detail = spotAdminService.getSpot(SPOT_ID);

        assertThat(detail.translation().title()).isEqualTo("Daelim Changgo");
        assertThat(detail.description()).isEqualTo("A 1970s rice warehouse.");
        assertThat(detail.mood().tags()).containsExactly(MoodTag.RETRO, MoodTag.LIVELY);
        assertThat(detail.bookmarkCount()).isEqualTo(1532L);
    }

    @Test
    @DisplayName("HIDDEN으로 바꾸면 사유·시각과 함께 숨겨진다")
    void change_status_hides() {
        Spot spot = SpotFixture.createWithId(SPOT_ID);
        spot.publish();
        when(spotRepository.findById(SPOT_ID)).thenReturn(Optional.of(spot));

        spotAdminService.changeStatus(SPOT_ID, SpotStatus.HIDDEN, "폐업 확인 중");

        assertThat(spot.getStatus()).isEqualTo(SpotStatus.HIDDEN);
        assertThat(spot.getStatusChangedAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
        verify(spotRepository).save(spot);
    }

    @Test
    @DisplayName("TAGGING_PENDING으로는 되돌릴 수 없다")
    void change_status_rejects_tagging_pending() {
        when(spotRepository.findById(SPOT_ID)).thenReturn(Optional.of(SpotFixture.createWithId(SPOT_ID)));

        assertThatThrownBy(() -> spotAdminService.changeStatus(SPOT_ID, SpotStatus.TAGGING_PENDING, null))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
        verify(spotRepository, never()).save(any());
    }

    @Test
    @DisplayName("무드 태그 보정은 중복을 제거하고 confidence를 1.0으로 올린다")
    void override_mood_tags() {
        SpotMood mood = SpotMoodFixture.create(SPOT_ID, List.of(MoodTag.RETRO));
        when(spotRepository.findById(SPOT_ID)).thenReturn(Optional.of(SpotFixture.createWithId(SPOT_ID)));
        when(spotMoodRepository.findBySpotId(SPOT_ID)).thenReturn(Optional.of(mood));

        spotAdminService.overrideMoodTags(SPOT_ID, List.of(MoodTag.COZY, MoodTag.LOCAL, MoodTag.COZY));

        assertThat(mood.getMoodTags()).containsExactly(MoodTag.COZY, MoodTag.LOCAL);
        assertThat(mood.getConfidence()).isEqualTo(1.0);
        verify(spotMoodRepository).save(mood);
    }

    @Test
    @DisplayName("태깅 전 스팟은 무드 태그를 보정할 수 없다")
    void override_mood_tags_without_mood_throws() {
        when(spotRepository.findById(SPOT_ID)).thenReturn(Optional.of(SpotFixture.createWithId(SPOT_ID)));
        when(spotMoodRepository.findBySpotId(SPOT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spotAdminService.overrideMoodTags(SPOT_ID, List.of(MoodTag.COZY)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SPOT_MOOD_NOT_FOUND);
    }

    @Test
    @DisplayName("설명이 없으면 새로 만들고, 있으면 내용을 바꾼다")
    void update_description_creates_or_updates() {
        when(spotRepository.findById(SPOT_ID)).thenReturn(Optional.of(SpotFixture.createWithId(SPOT_ID)));
        when(spotDescriptionRepository.findBySpotIdAndLocale(SPOT_ID, "en-US")).thenReturn(Optional.empty());

        spotAdminService.updateDescription(SPOT_ID, "  Hand-written description.  ");

        ArgumentCaptor<SpotDescription> captor = ArgumentCaptor.forClass(SpotDescription.class);
        verify(spotDescriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getLocale()).isEqualTo("en-US");
        assertThat(captor.getValue().getContent()).isEqualTo("Hand-written description.");
    }

    @Test
    @DisplayName("없는 스팟이면 실패한다")
    void unknown_spot_throws() {
        when(spotRepository.findById(SPOT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spotAdminService.changeRouteExclusion(SPOT_ID, true))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SPOT_NOT_FOUND);
    }

    private SpotAdminRow row(Long id) {
        return new SpotAdminRow(id, "title", SpotContentType.TOURIST_ATTRACTION, "서울", "성동구", SpotStatus.PUBLISHED,
                false, 0, LocalDateTime.now(FIXED_CLOCK), null);
    }
}
