package com.moodi.support.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.support.NoticeFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoticeTest {

    @Test
    @DisplayName("공지 생성 시 필드가 세팅된다")
    void create_sets_fields() {
        Notice notice = NoticeFixture.create();

        assertThat(notice.getType()).isEqualTo(NoticeType.ANNOUNCEMENT);
        assertThat(notice.getTitle()).isEqualTo("Route sharing is now available");
        assertThat(notice.isVisible()).isTrue();
        assertThat(notice.getPublishedAt()).isEqualTo(NoticeFixture.DEFAULT_PUBLISHED_AT);
    }

    @Test
    @DisplayName("제목이 100자를 넘으면 생성할 수 없다")
    void create_rejects_long_title() {
        assertThatThrownBy(() -> NoticeFixture.create(NoticeType.OTHER, "a".repeat(101), "content", true,
                LocalDate.of(2026, 8, 10)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("본문이 비어 있으면 생성할 수 없다")
    void create_rejects_blank_content() {
        assertThatThrownBy(() -> NoticeFixture.create(NoticeType.OTHER, "title", "  ", true,
                LocalDate.of(2026, 8, 10)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("수정 시 모든 필드가 교체된다")
    void update_replaces_fields() {
        Notice notice = NoticeFixture.create();

        notice.update(NoticeType.MAINTENANCE, "Scheduled maintenance", "Aug 5, 2:00-4:00 AM KST", false,
                LocalDate.of(2026, 8, 3));

        assertThat(notice.getType()).isEqualTo(NoticeType.MAINTENANCE);
        assertThat(notice.getTitle()).isEqualTo("Scheduled maintenance");
        assertThat(notice.getContent()).isEqualTo("Aug 5, 2:00-4:00 AM KST");
        assertThat(notice.isVisible()).isFalse();
        assertThat(notice.getPublishedAt()).isEqualTo(LocalDate.of(2026, 8, 3));
    }

    @Test
    @DisplayName("노출 상태를 바꿀 수 있다")
    void change_visibility_toggles() {
        Notice notice = NoticeFixture.create();

        notice.changeVisibility(false);

        assertThat(notice.isVisible()).isFalse();
    }

    @Test
    @DisplayName("미리보기는 줄바꿈을 공백으로 접는다")
    void preview_flattens_whitespace() {
        Notice notice = NoticeFixture.create();

        assertThat(notice.preview())
                .isEqualTo("You can now share your routes with friends. Open a route and tap Share.");
    }

    @Test
    @DisplayName("미리보기는 200자에서 잘린다")
    void preview_truncates_to_200_chars() {
        Notice notice = NoticeFixture.create(NoticeType.OTHER, "title", "x".repeat(300), true,
                LocalDate.of(2026, 8, 10));

        assertThat(notice.preview()).hasSize(Notice.PREVIEW_LENGTH);
    }
}
