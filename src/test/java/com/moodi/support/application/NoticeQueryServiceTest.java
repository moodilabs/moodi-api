package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.support.application.dto.NoticeDetail;
import com.moodi.support.application.dto.NoticeSummary;
import com.moodi.support.domain.NoticeRepository;
import com.moodi.support.support.NoticeFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeQueryServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeQueryRepository noticeQueryRepository;

    @InjectMocks
    private NoticeQueryService noticeQueryService;

    @Test
    @DisplayName("size보다 한 건 더 조회되면 다음 커서를 만든다")
    void get_visible_notices_builds_next_cursor_when_has_next() {
        when(noticeQueryRepository.findVisible(isNull(), isNull(), eq(3))).thenReturn(List.of(
                NoticeFixture.createWithId(3L, LocalDate.of(2026, 8, 10)),
                NoticeFixture.createWithId(2L, LocalDate.of(2026, 8, 3)),
                NoticeFixture.createWithId(1L, LocalDate.of(2026, 8, 1))));

        CursorResponse<NoticeSummary> result = noticeQueryService.getVisibleNotices(null, 2);

        assertThat(result.items()).extracting(NoticeSummary::id).containsExactly(3L, 2L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("2026-08-03,2");
    }

    @Test
    @DisplayName("커서를 파싱해 조회 조건으로 넘긴다")
    void get_visible_notices_passes_parsed_cursor() {
        when(noticeQueryRepository.findVisible(LocalDate.of(2026, 8, 3), 2L, 21))
                .thenReturn(List.of(NoticeFixture.createWithId(1L, LocalDate.of(2026, 8, 1))));

        CursorResponse<NoticeSummary> result = noticeQueryService.getVisibleNotices("2026-08-03,2", 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("공지가 없으면 빈 응답이다")
    void get_visible_notices_returns_empty() {
        when(noticeQueryRepository.findVisible(isNull(), isNull(), eq(21))).thenReturn(List.of());

        CursorResponse<NoticeSummary> result = noticeQueryService.getVisibleNotices(null, 20);

        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("잘못된 커서는 거부한다")
    void get_visible_notices_rejects_invalid_cursor() {
        assertThatThrownBy(() -> noticeQueryService.getVisibleNotices("bad-cursor", 20))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CURSOR_FORMAT);
    }

    @Test
    @DisplayName("노출 중인 공지 상세를 조회한다")
    void get_visible_notice_returns_detail() {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(NoticeFixture.createWithId(1L)));

        NoticeDetail detail = noticeQueryService.getVisibleNotice(1L);

        assertThat(detail.id()).isEqualTo(1L);
        assertThat(detail.title()).isEqualTo("Route sharing is now available");
    }

    @Test
    @DisplayName("숨김 공지는 앱에서 조회할 수 없다")
    void get_visible_notice_hides_hidden_notice() {
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(NoticeFixture.hidden()));

        assertThatThrownBy(() -> noticeQueryService.getVisibleNotice(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
    }

    @Test
    @DisplayName("발행일이 아직 오지 않은 공지는 id를 알아도 열리지 않는다")
    void get_visible_notice_hides_scheduled_notice() {
        // 목록에서 감춘 예약 공지가 상세로는 열리면 발행 전 내용이 그대로 새 나간다.
        when(noticeRepository.findById(1L))
                .thenReturn(Optional.of(NoticeFixture.createWithId(1L, LocalDate.now().plusDays(1))));

        assertThatThrownBy(() -> noticeQueryService.getVisibleNotice(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
    }

    @Test
    @DisplayName("오늘 발행된 공지는 열린다")
    void get_visible_notice_allows_today() {
        when(noticeRepository.findById(1L))
                .thenReturn(Optional.of(NoticeFixture.createWithId(1L, LocalDate.now())));

        NoticeDetail detail = noticeQueryService.getVisibleNotice(1L);

        assertThat(detail.id()).isEqualTo(1L);
    }
}
