package com.moodi.spot.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.spot.application.dto.BookmarkListRequest;
import com.moodi.spot.application.dto.BookmarkSortType;
import com.moodi.spot.application.dto.BookmarkSpotItem;
import com.moodi.spot.application.dto.BookmarkSpotRow;
import com.moodi.spot.application.dto.BookmarkToggleResult;
import com.moodi.spot.domain.Bookmark;
import com.moodi.spot.domain.BookmarkRepository;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotDescriptionRepository;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
import com.moodi.spot.application.BookmarkQueryRepository;
import com.moodi.spot.support.BookmarkFixture;
import com.moodi.spot.support.SpotFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private BookmarkQueryRepository bookmarkQueryRepository;

    @Mock
    private SpotRepository spotRepository;

    @Mock
    private SpotTranslationRepository spotTranslationRepository;

    @Mock
    private SpotDescriptionRepository spotDescriptionRepository;

    @Mock
    private SpotImageRepository spotImageRepository;

    @Mock
    private SpotMoodRepository spotMoodRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

    @Test
    @DisplayName("저장하지 않은 스팟을 토글하면 저장된다")
    void toggle_save_success() {
        UUID memberId = UUID.randomUUID();
        Long spotId = 1L;
        Spot spot = SpotFixture.createWithId(spotId);
        spot.publish();

        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));
        when(bookmarkRepository.findByMemberIdAndSpotId(memberId, spotId)).thenReturn(Optional.empty());
        when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(BookmarkFixture.createWithId(1L, memberId, spotId));
        when(bookmarkQueryRepository.countBySpotId(spotId)).thenReturn(1L);

        BookmarkToggleResult response = bookmarkService.toggle(memberId, spotId);

        assertThat(response.bookmarked()).isTrue();
        assertThat(response.spotId()).isEqualTo(spotId);
        assertThat(response.bookmarkCount()).isEqualTo(1L);
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("저장한 스팟을 다시 토글하면 해제된다")
    void toggle_unsave_success() {
        UUID memberId = UUID.randomUUID();
        Long spotId = 1L;
        Spot spot = SpotFixture.createWithId(spotId);
        Bookmark existing = BookmarkFixture.createWithId(1L, memberId, spotId);

        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));
        when(bookmarkRepository.findByMemberIdAndSpotId(memberId, spotId)).thenReturn(Optional.of(existing));
        when(bookmarkQueryRepository.countBySpotId(spotId)).thenReturn(0L);

        BookmarkToggleResult response = bookmarkService.toggle(memberId, spotId);

        assertThat(response.bookmarked()).isFalse();
        assertThat(response.bookmarkCount()).isEqualTo(0L);
        verify(bookmarkRepository).delete(existing);
    }

    @Test
    @DisplayName("존재하지 않는 스팟 저장 요청은 실패한다")
    void toggle_spot_not_found_throws() {
        UUID memberId = UUID.randomUUID();
        Long spotId = 999L;

        when(spotRepository.findById(spotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookmarkService.toggle(memberId, spotId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPOT_NOT_FOUND);
    }

    @Test
    @DisplayName("PUBLISHED가 아닌 스팟은 저장할 수 없다")
    void toggle_spot_not_available_throws() {
        UUID memberId = UUID.randomUUID();
        Long spotId = 1L;
        Spot spot = SpotFixture.createWithId(spotId); // TAGGING_PENDING 상태

        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));
        when(bookmarkRepository.findByMemberIdAndSpotId(memberId, spotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookmarkService.toggle(memberId, spotId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPOT_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("동시 중복 저장 시 유니크 제약 위반이 발생하면 이미 저장된 상태로 처리한다")
    void toggle_concurrent_duplicate_handled() {
        UUID memberId = UUID.randomUUID();
        Long spotId = 1L;
        Spot spot = SpotFixture.createWithId(spotId);
        spot.publish();

        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));
        when(bookmarkRepository.findByMemberIdAndSpotId(memberId, spotId)).thenReturn(Optional.empty());
        when(bookmarkRepository.save(any(Bookmark.class))).thenThrow(new DataIntegrityViolationException("unique constraint"));
        when(bookmarkQueryRepository.countBySpotId(spotId)).thenReturn(1L);

        BookmarkToggleResult response = bookmarkService.toggle(memberId, spotId);

        assertThat(response.bookmarked()).isTrue();
        assertThat(response.bookmarkCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("다른 회원은 같은 스팟을 각각 저장할 수 있다")
    void toggle_different_members_same_spot() {
        UUID memberA = UUID.randomUUID();
        UUID memberB = UUID.randomUUID();
        Long spotId = 1L;
        Spot spot = SpotFixture.createWithId(spotId);
        spot.publish();

        when(spotRepository.findById(spotId)).thenReturn(Optional.of(spot));
        when(bookmarkRepository.findByMemberIdAndSpotId(memberA, spotId)).thenReturn(Optional.empty());
        when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(BookmarkFixture.createWithId(1L, memberA, spotId));
        when(bookmarkQueryRepository.countBySpotId(spotId)).thenReturn(1L);

        BookmarkToggleResult responseA = bookmarkService.toggle(memberA, spotId);
        assertThat(responseA.bookmarked()).isTrue();

        when(bookmarkRepository.findByMemberIdAndSpotId(memberB, spotId)).thenReturn(Optional.empty());
        when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(BookmarkFixture.createWithId(2L, memberB, spotId));
        when(bookmarkQueryRepository.countBySpotId(spotId)).thenReturn(2L);

        BookmarkToggleResult responseB = bookmarkService.toggle(memberB, spotId);
        assertThat(responseB.bookmarked()).isTrue();
        assertThat(responseB.bookmarkCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("저장한 스팟 목록 제목은 en-US 로케일로 읽는다")
    void getBookmarks_reads_title_with_en_us_locale() {
        UUID memberId = UUID.randomUUID();
        Long spotId = 1L;
        BookmarkListRequest request = new BookmarkListRequest(
                null, null, BookmarkSortType.LATEST, null, null, null, null, 20);
        BookmarkSpotRow row = new BookmarkSpotRow(
                1L, spotId, "부산", "해운대구", 35.1, 129.1, LocalDateTime.now(), 1L);

        when(bookmarkQueryRepository.findByMemberLatest(
                eq(memberId), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(row));
        when(spotTranslationRepository.findBySpotIdInAndLocale(anyList(), anyString()))
                .thenReturn(List.of(SpotTranslation.create(spotId, "en-US", "Title", "Overview", "Addr1", "Addr2")));

        CursorResponse<BookmarkSpotItem> result = bookmarkService.getBookmarks(memberId, request);

        // 로케일을 걸지 않으면 한 스팟에 여러 번역 행이 있을 때 아무 행이나 뽑혀
        // Saved Spot 목록에 한국어 제목이 나갈 수 있다.
        verify(spotTranslationRepository).findBySpotIdInAndLocale(anyList(), eq("en-US"));
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().title()).isEqualTo("Title");
    }

    @Test
    @DisplayName("일괄 삭제 시 실제 삭제된 개수를 반환한다")
    void delete_bookmarks_returns_deleted_count() {
        UUID memberId = UUID.randomUUID();
        List<Long> spotIds = List.of(1L, 2L, 3L);

        when(bookmarkQueryRepository.deleteByMemberIdAndSpotIds(memberId, spotIds)).thenReturn(2);

        int deletedCount = bookmarkService.deleteBookmarks(memberId, spotIds);

        assertThat(deletedCount).isEqualTo(2);
    }
}
