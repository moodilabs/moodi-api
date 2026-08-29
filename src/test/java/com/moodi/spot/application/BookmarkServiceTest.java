package com.moodi.spot.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.spot.application.dto.BookmarkToggleResult;
import com.moodi.spot.domain.Bookmark;
import com.moodi.spot.domain.BookmarkRepository;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotDescriptionRepository;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("일괄 삭제 시 실제 삭제된 개수를 반환한다")
    void delete_bookmarks_returns_deleted_count() {
        UUID memberId = UUID.randomUUID();
        List<Long> spotIds = List.of(1L, 2L, 3L);

        when(bookmarkQueryRepository.deleteByMemberIdAndSpotIds(memberId, spotIds)).thenReturn(2);

        int deletedCount = bookmarkService.deleteBookmarks(memberId, spotIds);

        assertThat(deletedCount).isEqualTo(2);
    }
}
