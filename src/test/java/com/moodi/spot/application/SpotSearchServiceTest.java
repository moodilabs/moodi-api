package com.moodi.spot.application;

import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.response.CursorResponse;
import com.moodi.spot.application.dto.SpotSearchItem;
import com.moodi.spot.application.dto.SpotSearchRequest;
import com.moodi.spot.application.dto.SpotSearchRow;
import com.moodi.spot.application.dto.SpotSearchSortType;
import com.moodi.spot.support.SpotSearchRequestFixture;
import com.moodi.spot.support.SpotSearchRowFixture;
import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotSearchServiceTest {

    @Mock
    private SpotSearchQueryRepository spotSearchQueryRepository;

    @Mock
    private BookmarkQueryRepository bookmarkQueryRepository;

    @Mock
    private SpotTranslationRepository spotTranslationRepository;

    @Mock
    private SpotDescriptionRepository spotDescriptionRepository;

    @Mock
    private SpotImageRepository spotImageRepository;

    @Mock
    private SpotMoodRepository spotMoodRepository;

    @InjectMocks
    private SpotSearchService spotSearchService;

    @Test
    @DisplayName("검색어 없이 조회하면 MOST_SAVED 정렬로 조회된다")
    void search_without_keyword_uses_most_saved() {
        // given
        SpotSearchRequest request = SpotSearchRequestFixture.createMostSaved();

        List<SpotSearchRow> rows = List.of(
                SpotSearchRowFixture.create(8317L, "남구", 5L),
                SpotSearchRowFixture.create(8326L, "수영구", 3L)
        );

        when(spotSearchQueryRepository.searchByMostSaved(
                any(), any(), any(), anyBoolean(), any(), any(), any(), anyInt()
        )).thenReturn(rows);
        stubEnrichment();

        // when
        CursorResponse<SpotSearchItem> result = spotSearchService.search(null, request);

        // then
        assertThat(result.items()).hasSize(2);
        verify(spotSearchQueryRepository).searchByMostSaved(
                any(), any(), any(), anyBoolean(), any(), any(), any(), anyInt());
        verify(spotSearchQueryRepository, never()).searchByBestMatch(
                any(), any(), any(), anyBoolean(), any(), any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("검색어 + BEST_MATCH 정렬로 조회된다")
    void search_with_keyword_best_match() {
        // given
        SpotSearchRequest request = SpotSearchRequestFixture.createBestMatch("부산");

        List<SpotSearchRow> rows = List.of(
                SpotSearchRowFixture.create(8317L, "남구", 5L, 3),
                SpotSearchRowFixture.create(8326L, "수영구", 3L, 2)
        );

        when(spotSearchQueryRepository.searchByBestMatch(
                eq("부산"), any(), any(), anyBoolean(), any(),
                any(), any(), any(), anyInt()
        )).thenReturn(rows);
        stubEnrichment();

        // when
        CursorResponse<SpotSearchItem> result = spotSearchService.search(null, request);

        // then
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).spotId()).isEqualTo(8317L);
        verify(spotSearchQueryRepository).searchByBestMatch(
                eq("부산"), any(), any(), anyBoolean(), any(),
                any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("로그인 사용자의 북마크 여부가 응답에 포함된다")
    void search_with_logged_in_member_includes_bookmark_status() {
        // given
        UUID memberId = UUID.randomUUID();
        SpotSearchRequest request = SpotSearchRequestFixture.createMostSaved();

        List<SpotSearchRow> rows = List.of(
                SpotSearchRowFixture.create(8317L, "남구", 5L),
                SpotSearchRowFixture.create(8326L, "수영구", 3L)
        );

        when(spotSearchQueryRepository.searchByMostSaved(
                any(), any(), any(), anyBoolean(), eq(memberId), any(), any(), anyInt()
        )).thenReturn(rows);
        stubEnrichment();
        when(bookmarkQueryRepository.findBookmarkedSpotIds(eq(memberId), anyList()))
                .thenReturn(Set.of(8317L));

        // when
        CursorResponse<SpotSearchItem> result = spotSearchService.search(memberId, request);

        // then
        assertThat(result.items().get(0).bookmarked()).isTrue();
        assertThat(result.items().get(1).bookmarked()).isFalse();
        verify(bookmarkQueryRepository).findBookmarkedSpotIds(eq(memberId), anyList());
    }

    @Test
    @DisplayName("비로그인 시 bookmarked는 항상 false")
    void search_without_member_bookmark_always_false() {
        // given
        SpotSearchRequest request = SpotSearchRequestFixture.createMostSaved();

        List<SpotSearchRow> rows = List.of(
                SpotSearchRowFixture.create()
        );

        when(spotSearchQueryRepository.searchByMostSaved(
                any(), any(), any(), anyBoolean(), isNull(), any(), any(), anyInt()
        )).thenReturn(rows);
        stubEnrichment();

        // when
        CursorResponse<SpotSearchItem> result = spotSearchService.search(null, request);

        // then
        assertThat(result.items().get(0).bookmarked()).isFalse();
        verify(bookmarkQueryRepository, never()).findBookmarkedSpotIds(any(), anyList());
    }

    @Test
    @DisplayName("routePublicId가 있으면 isInRoute가 응답에 포함된다")
    void search_with_route_public_id_includes_in_route() {
        // given
        UUID routePublicId = UUID.randomUUID();
        SpotSearchRequest request = SpotSearchRequestFixture.createMostSaved(routePublicId);

        List<SpotSearchRow> rows = List.of(
                SpotSearchRowFixture.create(8317L, "남구", 5L),
                SpotSearchRowFixture.create(516L, "서울", "종로구", 1L)
        );

        when(spotSearchQueryRepository.searchByMostSaved(
                any(), any(), any(), anyBoolean(), any(), any(), any(), anyInt()
        )).thenReturn(rows);
        stubEnrichment();
        when(spotSearchQueryRepository.findSpotIdsInRoute(eq(routePublicId), anyList()))
                .thenReturn(Set.of(8317L));

        // when
        CursorResponse<SpotSearchItem> result = spotSearchService.search(null, request);

        // then
        assertThat(result.items().get(0).inRoute()).isTrue();
        assertThat(result.items().get(1).inRoute()).isFalse();
    }

    @Test
    @DisplayName("routePublicId가 없으면 isInRoute는 항상 false")
    void search_without_route_public_id_in_route_always_false() {
        // given
        SpotSearchRequest request = SpotSearchRequestFixture.createMostSaved();

        List<SpotSearchRow> rows = List.of(
                SpotSearchRowFixture.create()
        );

        when(spotSearchQueryRepository.searchByMostSaved(
                any(), any(), any(), anyBoolean(), any(), any(), any(), anyInt()
        )).thenReturn(rows);
        stubEnrichment();

        // when
        CursorResponse<SpotSearchItem> result = spotSearchService.search(null, request);

        // then
        assertThat(result.items().get(0).inRoute()).isFalse();
        verify(spotSearchQueryRepository, never()).findSpotIdsInRoute(any(), anyList());
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 CursorResponse를 반환한다")
    void search_empty_result_returns_empty_cursor_response() {
        // given
        SpotSearchRequest request = SpotSearchRequestFixture.createBestMatch("없는키워드");

        when(spotSearchQueryRepository.searchByBestMatch(
                eq("없는키워드"), any(), any(), anyBoolean(), any(),
                any(), any(), any(), anyInt()
        )).thenReturn(List.of());

        // when
        CursorResponse<SpotSearchItem> result = spotSearchService.search(null, request);

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("size+1개 반환 시 hasNext=true이고 nextCursor가 생성된다")
    void search_paging_has_next_true_when_extra_row_exists() {
        // given
        int size = 2;
        SpotSearchRequest request = SpotSearchRequestFixture.createMostSaved(size);

        // size+1 = 3개 반환
        List<SpotSearchRow> rows = List.of(
                SpotSearchRowFixture.create(8317L, "남구", 5L),
                SpotSearchRowFixture.create(8326L, "수영구", 3L),
                SpotSearchRowFixture.create(516L, "서울", "종로구", 1L)
        );

        when(spotSearchQueryRepository.searchByMostSaved(
                any(), any(), any(), anyBoolean(), any(), any(), any(), anyInt()
        )).thenReturn(rows);
        stubEnrichment();

        // when
        CursorResponse<SpotSearchItem> result = spotSearchService.search(null, request);

        // then
        assertThat(result.items()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("3,8326");
    }

    private void stubEnrichment() {
        when(spotTranslationRepository.findBySpotIdIn(anyList()))
                .thenAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);
                    return ids.stream()
                            .map(id -> SpotTranslation.create(id, "en-US", "Title " + id, "Overview", "Addr1", "Addr2"))
                            .toList();
                });

        when(spotDescriptionRepository.findBySpotIdInAndLocale(anyList(), anyString()))
                .thenAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);
                    return ids.stream()
                            .map(id -> SpotDescription.create(id, "en-US", "Description for " + id))
                            .toList();
                });

        when(spotImageRepository.findBySpotIdInAndIsPrimaryTrue(anyList()))
                .thenAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);
                    return ids.stream()
                            .map(id -> SpotImage.createPrimary(id, "https://img.moodi.kr/" + id + ".jpg"))
                            .toList();
                });

        when(spotMoodRepository.findBySpotIdIn(anyList()))
                .thenAnswer(invocation -> {
                    List<Long> ids = invocation.getArgument(0);
                    return ids.stream()
                            .map(id -> SpotMood.create(id, null, List.of(MoodTag.MODERN, MoodTag.COZY), 0.9))
                            .toList();
                });
    }
}
