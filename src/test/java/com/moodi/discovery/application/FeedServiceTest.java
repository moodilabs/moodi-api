package com.moodi.discovery.application;

import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.response.CursorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 3, 12, 0);
    private static final int PAGE_SIZE = 20;
    private static final int IMPRESSION_WINDOW_DAYS = 30;
    private static final String GENERATED_SEED = "new-seed";

    @Mock
    private FeedSpotReader feedSpotReader;
    @Mock
    private PopularSpotReader popularSpotReader;
    @Mock
    private PreferredMoodReader preferredMoodReader;
    @Mock
    private FeedImpressionRepository feedImpressionRepository;
    @Mock
    private FeedSeedGenerator seedGenerator;

    @Captor
    private ArgumentCaptor<FeedQuery> queryCaptor;

    private FeedService feedService;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(KST).toInstant(), KST);
        feedService = new FeedService(feedSpotReader, popularSpotReader, preferredMoodReader,
                feedImpressionRepository, seedGenerator,
                new FeedProperties(IMPRESSION_WINDOW_DAYS), clock);
        memberId = UUID.randomUUID();
    }

    @Test
    @DisplayName("커서가 없으면 새로고침으로 보고 새 시드를 발급한다")
    void issues_new_seed_without_cursor() {
        givenMemberWithMoods(MoodTag.COZY);
        given(feedSpotReader.readPersonalizedFeed(any())).willReturn(List.of());

        feedService.getFeed(memberId, null);

        verify(feedSpotReader).readPersonalizedFeed(queryCaptor.capture());
        assertThat(queryCaptor.getValue().seed()).isEqualTo(GENERATED_SEED);
        assertThat(queryCaptor.getValue().sessionAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("커서가 있으면 시드와 세션 시각을 이어 써서 스크롤 중 순서를 고정한다")
    void reuses_seed_and_session_from_cursor() {
        LocalDateTime sessionAt = NOW.minusMinutes(5);
        String cursor = new FeedCursor("kept-seed", sessionAt, 0, 2L, "abc", 7L).encode();
        given(preferredMoodReader.readByMemberId(memberId)).willReturn(List.of(MoodTag.COZY));
        given(feedSpotReader.readPersonalizedFeed(any())).willReturn(List.of());

        feedService.getFeed(memberId, cursor);

        verify(feedSpotReader).readPersonalizedFeed(queryCaptor.capture());
        FeedQuery query = queryCaptor.getValue();
        assertThat(query.seed()).isEqualTo("kept-seed");
        assertThat(query.sessionAt()).isEqualTo(sessionAt);
        assertThat(query.cursor().spotId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("노출 이력 유효 기간은 설정값으로 계산된다")
    void applies_configured_impression_window() {
        givenMemberWithMoods(MoodTag.COZY);
        given(feedSpotReader.readPersonalizedFeed(any())).willReturn(List.of());

        feedService.getFeed(memberId, null);

        verify(feedSpotReader).readPersonalizedFeed(queryCaptor.capture());
        assertThat(queryCaptor.getValue().impressionFrom())
                .isEqualTo(NOW.minusDays(IMPRESSION_WINDOW_DAYS));
    }

    @Test
    @DisplayName("응답에 나간 스팟의 노출 이력을 기록한다")
    void records_impressions_for_returned_spots() {
        givenMemberWithMoods(MoodTag.COZY);
        given(feedSpotReader.readPersonalizedFeed(any())).willReturn(rows(3));

        feedService.getFeed(memberId, null);

        verify(feedImpressionRepository).recordShown(memberId, List.of(0L, 1L, 2L), NOW);
    }

    @Test
    @DisplayName("비회원은 개인화 없는 피드를 받고 노출 이력을 남기지 않는다")
    void guest_reads_guest_feed_without_impressions() {
        given(seedGenerator.generate()).willReturn(GENERATED_SEED);
        given(feedSpotReader.readGuestFeed(any())).willReturn(rows(2));

        CursorResponse<FeedSpotItem> response = feedService.getFeed(null, null);

        assertThat(response.items()).hasSize(2);
        verify(feedSpotReader, never()).readPersonalizedFeed(any());
        verify(feedImpressionRepository, never()).recordShown(any(), any(), any());
        verify(preferredMoodReader, never()).readByMemberId(any());
    }

    @Test
    @DisplayName("페이지 크기를 넘게 조회되면 초과분은 잘라내고 hasNext가 true다")
    void marks_has_next_when_more_rows_exist() {
        givenMemberWithMoods(MoodTag.COZY);
        given(feedSpotReader.readPersonalizedFeed(any())).willReturn(rows(PAGE_SIZE + 1));

        CursorResponse<FeedSpotItem> response = feedService.getFeed(memberId, null);

        assertThat(response.items()).hasSize(PAGE_SIZE);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isNotNull();
    }

    @Test
    @DisplayName("마지막 페이지면 hasNext가 false이고 커서를 주지 않는다")
    void marks_last_page_without_cursor() {
        givenMemberWithMoods(MoodTag.COZY);
        given(feedSpotReader.readPersonalizedFeed(any())).willReturn(rows(3));

        CursorResponse<FeedSpotItem> response = feedService.getFeed(memberId, null);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("다음 커서는 마지막 행의 정렬 키를 담는다")
    void next_cursor_carries_last_row_sort_keys() {
        givenMemberWithMoods(MoodTag.COZY);
        given(feedSpotReader.readPersonalizedFeed(any())).willReturn(rows(PAGE_SIZE + 1));

        CursorResponse<FeedSpotItem> response = feedService.getFeed(memberId, null);

        FeedCursor cursor = FeedCursor.decode(response.nextCursor());
        assertThat(cursor.spotId()).isEqualTo(PAGE_SIZE - 1);
        assertThat(cursor.seed()).isEqualTo(GENERATED_SEED);
        assertThat(cursor.sessionAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("추천이 소진되면 빈 응답을 준다")
    void returns_empty_response_when_exhausted() {
        givenMemberWithMoods(MoodTag.COZY);
        given(feedSpotReader.readPersonalizedFeed(any())).willReturn(List.of());

        CursorResponse<FeedSpotItem> response = feedService.getFeed(memberId, null);

        assertThat(response.items()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        verify(feedImpressionRepository, never()).recordShown(any(), any(), any());
    }

    @Test
    @DisplayName("선호 무드는 스팟 무드 태그와 같은 key 형식으로 넘긴다")
    void passes_preferred_moods_as_tag_keys() {
        givenMemberWithMoods(MoodTag.GOLDEN_HOUR, MoodTag.COZY);
        given(feedSpotReader.readPersonalizedFeed(any())).willReturn(List.of());

        feedService.getFeed(memberId, null);

        verify(feedSpotReader).readPersonalizedFeed(queryCaptor.capture());
        assertThat(queryCaptor.getValue().moodTagKeys()).containsExactly("golden_hour", "cozy");
    }

    @Test
    @DisplayName("인기 스팟은 정확히 5개까지만 조회한다")
    void reads_exactly_five_popular_spots() {
        given(popularSpotReader.readTopByBookmarkCount(memberId, 5)).willReturn(List.of(
                new PopularSpotRow(1L, "스팟", "https://img/1", "서울", 10L, true)));

        List<PopularSpotItem> items = feedService.getPopularSpots(memberId);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).bookmarkCount()).isEqualTo(10L);
        verify(popularSpotReader).readTopByBookmarkCount(memberId, 5);
    }

    private void givenMemberWithMoods(MoodTag... moods) {
        given(seedGenerator.generate()).willReturn(GENERATED_SEED);
        given(preferredMoodReader.readByMemberId(memberId)).willReturn(List.of(moods));
    }

    private List<FeedSpotRow> rows(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new FeedSpotRow(i, "스팟-" + i, "https://img/" + i, "서울",
                        false, 0, 1L, "hash-" + i))
                .toList();
    }
}
