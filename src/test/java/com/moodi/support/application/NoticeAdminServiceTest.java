package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.support.application.dto.NoticeCommand;
import com.moodi.support.application.dto.NoticeDetail;
import com.moodi.support.domain.Notice;
import com.moodi.support.domain.NoticeRepository;
import com.moodi.support.domain.NoticeType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeAdminServiceTest {

    private static final NoticeCommand COMMAND = new NoticeCommand(NoticeType.UPDATE, "Transit times updated",
            "Transit times may be inaccurate for some areas.", true, LocalDate.of(2026, 8, 3));

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeQueryRepository noticeQueryRepository;

    @InjectMocks
    private NoticeAdminService noticeAdminService;

    @Test
    @DisplayName("공지 등록 시 저장된 ID를 돌려준다")
    void create_returns_saved_id() {
        when(noticeRepository.save(any(Notice.class))).thenReturn(NoticeFixture.createWithId(7L));

        Long id = noticeAdminService.create(COMMAND);

        assertThat(id).isEqualTo(7L);
    }

    @Test
    @DisplayName("공지 수정 시 필드가 교체되고 저장된다")
    void update_replaces_and_saves() {
        Notice notice = NoticeFixture.createWithId(1L);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        noticeAdminService.update(1L, COMMAND);

        assertThat(notice.getType()).isEqualTo(NoticeType.UPDATE);
        assertThat(notice.getTitle()).isEqualTo("Transit times updated");
        verify(noticeRepository).save(notice);
    }

    @Test
    @DisplayName("노출 상태를 바꾸고 저장한다")
    void change_visibility_saves() {
        Notice notice = NoticeFixture.createWithId(1L);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        noticeAdminService.changeVisibility(1L, false);

        assertThat(notice.isVisible()).isFalse();
        verify(noticeRepository).save(notice);
    }

    @Test
    @DisplayName("공지를 삭제한다")
    void delete_removes_notice() {
        Notice notice = NoticeFixture.createWithId(1L);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));

        noticeAdminService.delete(1L);

        verify(noticeRepository).delete(notice);
    }

    @Test
    @DisplayName("없는 공지를 수정하면 실패한다")
    void update_unknown_notice_throws() {
        when(noticeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noticeAdminService.update(99L, COMMAND))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOTICE_NOT_FOUND);
    }

    @Test
    @DisplayName("어드민 목록은 숨김 공지도 포함하고 필터를 그대로 넘긴다")
    void get_notices_passes_filters() {
        when(noticeQueryRepository.findAll(eq(NoticeType.ANNOUNCEMENT), eq(false), isNull(), isNull(), eq(21)))
                .thenReturn(List.of(NoticeFixture.hidden()));

        CursorResponse<NoticeDetail> result = noticeAdminService.getNotices(NoticeType.ANNOUNCEMENT, false, null, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().visible()).isFalse();
    }
}
