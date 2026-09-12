package com.moodi.support.domain;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.support.InquiryFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 9, 0);

    @Test
    @DisplayName("등록 직후 상태는 RECEIVED이고 답변이 없다")
    void create_starts_received() {
        Inquiry inquiry = InquiryFixture.create(MEMBER_ID);

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.RECEIVED);
        assertThat(inquiry.isAnswered()).isFalse();
        assertThat(inquiry.isOwnedBy(MEMBER_ID)).isTrue();
        assertThat(inquiry.isOwnedBy(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("첨부가 5개를 넘으면 등록할 수 없다")
    void create_rejects_too_many_attachments() {
        List<InquiryAttachment> six = Collections.nCopies(6, InquiryAttachment.of("k", "image/jpeg", 0));

        assertThatThrownBy(() -> Inquiry.create(MEMBER_ID, InquiryTopic.OTHER, "s", "c", six))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_TOO_MANY_ATTACHMENTS);
    }

    @Test
    @DisplayName("제목이 60자를 넘으면 등록할 수 없다")
    void create_rejects_long_subject() {
        assertThatThrownBy(() -> Inquiry.create(MEMBER_ID, InquiryTopic.OTHER, "s".repeat(61), "c", List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("답변하면 ANSWERED가 되고 답변자·시각이 남는다")
    void answer_marks_answered() {
        Inquiry inquiry = InquiryFixture.create(MEMBER_ID);
        UUID adminId = UUID.randomUUID();

        inquiry.answer("Thanks for letting us know.", adminId, NOW);

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(inquiry.getAnswerContent()).isEqualTo("Thanks for letting us know.");
        assertThat(inquiry.getAnsweredBy()).isEqualTo(adminId);
        assertThat(inquiry.getAnsweredAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("빈 답변은 등록할 수 없다")
    void answer_rejects_blank() {
        Inquiry inquiry = InquiryFixture.create(MEMBER_ID);

        assertThatThrownBy(() -> inquiry.answer(" ", UUID.randomUUID(), NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThat(inquiry.isAnswered()).isFalse();
    }
}
