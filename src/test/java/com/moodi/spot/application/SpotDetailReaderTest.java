package com.moodi.spot.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.mood.MoodTag;
import com.moodi.spot.application.dto.PopularAreaSpotItem;
import com.moodi.spot.application.dto.SimilarMoodSpotItem;
import com.moodi.spot.application.dto.SpotDetailSnapshot;
import com.moodi.spot.domain.BookmarkRepository;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslationRepository;
import com.moodi.spot.support.SpotFixture;
import com.moodi.spot.support.SpotImageFixture;
import com.moodi.spot.support.SpotMoodFixture;
import com.moodi.spot.support.SpotTranslationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotDetailReaderTest {

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotTranslationRepository translationRepository;

    @Mock
    private SpotImageRepository imageRepository;

    @Mock
    private SpotMoodRepository moodRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private BookmarkQueryRepository bookmarkQueryRepository;

    @Mock
    private SpotDetailQueryRepository spotDetailQueryRepository;

    @InjectMocks
    private SpotDetailReader spotDetailReader;

    @Test
    @DisplayName("존재하지 않는 스팟 조회 시 SPOT_NOT_FOUND 예외가 발생한다")
    void read_spot_not_found() {
        // given
        Long spotId = 999L;
        when(spotRepository.findById(spotId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> spotDetailReader.read(spotId, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPOT_NOT_FOUND);
    }

    @Test
    @DisplayName("PUBLISHED가 아닌 스팟은 조회할 수 없다")
    void read_unpublished_spot_not_found() {
        // given
        Long spotId = 1L;
        Spot spot = SpotFixture.createWithId(spotId);

        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));

        // when & then
        assertThatThrownBy(() -> spotDetailReader.read(spotId, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPOT_NOT_FOUND);
    }

    @Test
    @DisplayName("로그인 사용자는 북마크 여부가 반영된다")
    void read_with_member_bookmarked_true() {
        // given
        Long spotId = 1L;
        UUID memberId = UUID.randomUUID();
        stubPublishedSpot(spotId);
        stubEmptyRecommendations(spotId);
        when(bookmarkRepository.existsByMemberIdAndSpotId(memberId, spotId)).thenReturn(true);

        // when
        SpotDetailSnapshot snapshot = spotDetailReader.read(spotId, memberId);

        // then
        assertThat(snapshot.bookmarked()).isTrue();
        assertThat(snapshot.bookmarkCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("비회원은 bookmarked가 false이다")
    void read_without_member_bookmarked_false() {
        // given
        Long spotId = 1L;
        stubPublishedSpot(spotId);
        stubEmptyRecommendations(spotId);

        // when
        SpotDetailSnapshot snapshot = spotDetailReader.read(spotId, null);

        // then
        assertThat(snapshot.bookmarked()).isFalse();
        verify(bookmarkRepository, never()).existsByMemberIdAndSpotId(any(), anyLong());
    }

    @Test
    @DisplayName("유사 무드 스팟과 지역 인기 스팟이 Snapshot에 포함된다")
    void read_includes_recommendations() {
        // given
        Long spotId = 1L;
        stubPublishedSpot(spotId);

        List<SimilarMoodSpotItem> similarSpots = List.of(
                new SimilarMoodSpotItem(2L, "스팟B", "img.jpg", "서울", 10L)
        );
        List<PopularAreaSpotItem> popularSpots = List.of(
                new PopularAreaSpotItem(3L, "스팟C", "img2.jpg", List.of("#Cozy"))
        );

        when(spotDetailQueryRepository.findSimilarMoodSpots(eq(spotId), anyList(), anyInt()))
                .thenReturn(similarSpots);
        when(spotDetailQueryRepository.findPopularSpotsByArea(eq(spotId), anyString(), anyString(), anyInt()))
                .thenReturn(popularSpots);

        // when
        SpotDetailSnapshot snapshot = spotDetailReader.read(spotId, null);

        // then
        assertThat(snapshot.similarMoodSpots()).hasSize(1);
        assertThat(snapshot.similarMoodSpots().getFirst().spotId()).isEqualTo(2L);
        assertThat(snapshot.popularAreaSpots()).hasSize(1);
        assertThat(snapshot.popularAreaSpots().getFirst().spotId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("무드 태그 키가 추천 쿼리에 전달된다")
    void read_passes_mood_tag_keys_to_query() {
        // given
        Long spotId = 1L;
        Spot spot = SpotFixture.createWithId(spotId);
        spot.publish();

        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));
        when(translationRepository.findBySpotIdAndLocale(spotId, "ko-KR"))
                .thenReturn(Optional.of(SpotTranslationFixture.create(spotId)));
        when(imageRepository.findBySpotId(spotId)).thenReturn(List.of());
        when(moodRepository.findBySpotId(spotId))
                .thenReturn(Optional.of(SpotMoodFixture.create(spotId,
                        List.of(MoodTag.NATURE, MoodTag.OCEAN))));
        when(bookmarkQueryRepository.countBySpotId(spotId)).thenReturn(0L);
        when(spotDetailQueryRepository.findSimilarMoodSpots(anyLong(), anyList(), anyInt()))
                .thenReturn(List.of());
        when(spotDetailQueryRepository.findPopularSpotsByArea(anyLong(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        // when
        spotDetailReader.read(spotId, null);

        // then
        verify(spotDetailQueryRepository).findSimilarMoodSpots(
                eq(spotId), eq(List.of("nature", "ocean")), eq(5));
    }

    private void stubPublishedSpot(Long spotId) {
        Spot spot = SpotFixture.createWithId(spotId);
        spot.publish();

        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));
        when(translationRepository.findBySpotIdAndLocale(spotId, "ko-KR"))
                .thenReturn(Optional.of(SpotTranslationFixture.create(spotId)));
        when(imageRepository.findBySpotId(spotId))
                .thenReturn(List.of(SpotImageFixture.createPrimary(spotId)));
        when(moodRepository.findBySpotId(spotId))
                .thenReturn(Optional.of(SpotMoodFixture.create(spotId)));
        when(bookmarkQueryRepository.countBySpotId(spotId)).thenReturn(3L);
    }

    private void stubEmptyRecommendations(Long spotId) {
        when(spotDetailQueryRepository.findSimilarMoodSpots(eq(spotId), anyList(), anyInt()))
                .thenReturn(List.of());
        when(spotDetailQueryRepository.findPopularSpotsByArea(eq(spotId), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());
    }
}
