package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.support.application.dto.AdminInquiryDetail;
import com.moodi.support.application.dto.AdminInquirySummary;
import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryRepository;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.support.InquiryFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryAdminServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID WITHDRAWN_MEMBER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID INQUIRY_ID = UUID.randomUUID();
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 3, 10, 0);

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private InquiryQueryRepository inquiryQueryRepository;

    @Mock
    private InquiryAttachmentStorage attachmentStorage;

    @Mock
    private MemberSummaryReader memberSummaryReader;

    private InquiryAdminService inquiryAdminService;

    @BeforeEach
    void setUp() {
        inquiryAdminService = new InquiryAdminService(inquiryRepository, inquiryQueryRepository, attachmentStorage,
                memberSummaryReader, FIXED_CLOCK);
    }

    @Test
    @DisplayName("목록에 회원 닉네임·이메일을 붙이고 탈퇴 회원은 표시한다")
    void get_inquiries_attaches_member_summaries() {
        when(inquiryQueryRepository.findAll(eq(InquiryStatus.RECEIVED), isNull(), isNull(), isNull(), eq(21)))
                .thenReturn(List.of(
                        InquiryFixture.createWithId(INQUIRY_ID, MEMBER_ID, CREATED_AT),
                        InquiryFixture.createWithId(UUID.randomUUID(), WITHDRAWN_MEMBER_ID, CREATED_AT.minusDays(1))));
        when(memberSummaryReader.readByIds(any())).thenReturn(Map.of(
                MEMBER_ID, new MemberSummaryReader.MemberSummary(MEMBER_ID, "moi", "moi@moodi.kr", false),
                WITHDRAWN_MEMBER_ID, new MemberSummaryReader.MemberSummary(WITHDRAWN_MEMBER_ID, null, null, true)));

        CursorResponse<AdminInquirySummary> result = inquiryAdminService.getInquiries(InquiryStatus.RECEIVED, null,
                null, 20);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().getFirst().member().nickname()).isEqualTo("moi");
        assertThat(result.items().get(1).member().withdrawn()).isTrue();
        assertThat(result.items().get(1).member().nickname()).isNull();
    }

    @Test
    @DisplayName("답변하면 상태가 ANSWERED가 되고 저장된다")
    void answer_marks_answered_and_saves() {
        Inquiry inquiry = InquiryFixture.createWithId(INQUIRY_ID, MEMBER_ID, CREATED_AT);
        when(inquiryRepository.findById(INQUIRY_ID)).thenReturn(Optional.of(inquiry));

        inquiryAdminService.answer(INQUIRY_ID, ADMIN_ID, "Thanks for letting us know.");

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(inquiry.getAnsweredBy()).isEqualTo(ADMIN_ID);
        assertThat(inquiry.getAnsweredAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
        verify(inquiryRepository).save(inquiry);
    }

    @Test
    @DisplayName("상세는 첨부 URL·회원·답변을 포함한다")
    void get_includes_attachments_member_and_answer() {
        Inquiry inquiry = InquiryFixture.createWithAttachment(MEMBER_ID);
        inquiry.answer("Done.", ADMIN_ID, CREATED_AT);
        when(inquiryRepository.findById(INQUIRY_ID)).thenReturn(Optional.of(inquiry));
        when(attachmentStorage.issueReadUrl(any())).thenReturn("https://read");
        when(memberSummaryReader.readByIds(any())).thenReturn(Map.of(
                MEMBER_ID, new MemberSummaryReader.MemberSummary(MEMBER_ID, "moi", "moi@moodi.kr", false)));

        AdminInquiryDetail detail = inquiryAdminService.get(INQUIRY_ID);

        assertThat(detail.attachments()).extracting("url").containsExactly("https://read");
        assertThat(detail.member().email()).isEqualTo("moi@moodi.kr");
        assertThat(detail.answer().answeredBy()).isEqualTo(ADMIN_ID);
    }

    @Test
    @DisplayName("없는 문의에 답변하면 실패한다")
    void answer_unknown_inquiry_throws() {
        when(inquiryRepository.findById(INQUIRY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryAdminService.answer(INQUIRY_ID, ADMIN_ID, "x"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_NOT_FOUND);
    }

    @Test
    @DisplayName("미답변 수를 센다")
    void count_received() {
        when(inquiryRepository.countByStatus(InquiryStatus.RECEIVED)).thenReturn(4L);

        assertThat(inquiryAdminService.countReceived()).isEqualTo(4L);
    }
}
