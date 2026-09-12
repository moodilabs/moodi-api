package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import com.moodi.support.application.dto.InquiryCreateCommand;
import com.moodi.support.application.dto.InquiryDetail;
import com.moodi.support.application.dto.InquirySummary;
import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryRepository;
import com.moodi.support.domain.InquiryTopic;
import com.moodi.support.support.InquiryFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID INQUIRY_ID = UUID.randomUUID();
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 3, 10, 0);

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private InquiryQueryRepository inquiryQueryRepository;

    @Mock
    private InquiryAttachmentStorage attachmentStorage;

    @InjectMocks
    private InquiryService inquiryService;

    @Test
    @DisplayName("업로드 URL은 검증된 형식과 회원 경로로 발급한다")
    void issue_upload_url_uses_validated_file() {
        when(attachmentStorage.issueUploadUrl(startsWith("inquiries/" + MEMBER_ID + "/"), eq("image/jpeg"), eq(1024L)))
                .thenReturn(new InquiryAttachmentStorage.UploadTarget("https://signed", "inquiries/x/y.jpg", 300));

        InquiryAttachmentStorage.UploadTarget target = inquiryService.issueUploadUrl(MEMBER_ID, "image/jpeg", 1024);

        assertThat(target.uploadUrl()).isEqualTo("https://signed");
    }

    @Test
    @DisplayName("문의 등록 시 첨부를 순서대로 붙여 저장한다")
    void create_saves_with_attachments_in_order() {
        String prefix = "inquiries/" + MEMBER_ID + "/";
        when(inquiryRepository.save(any(Inquiry.class)))
                .thenReturn(InquiryFixture.createWithId(INQUIRY_ID, MEMBER_ID, CREATED_AT));

        UUID id = inquiryService.create(MEMBER_ID, new InquiryCreateCommand(InquiryTopic.ROUTES, "subject", "content",
                List.of(new InquiryCreateCommand.Attachment(prefix + "a.jpg", "image/jpeg"),
                        new InquiryCreateCommand.Attachment(prefix + "b.mp4", "video/mp4"))));

        ArgumentCaptor<Inquiry> captor = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository).save(captor.capture());
        assertThat(captor.getValue().getAttachments()).hasSize(2);
        assertThat(captor.getValue().getAttachments().get(1).getObjectKey()).isEqualTo(prefix + "b.mp4");
        assertThat(captor.getValue().getAttachments().get(1).getSortOrder()).isEqualTo(1);
        assertThat(id).isEqualTo(INQUIRY_ID);
    }

    @Test
    @DisplayName("다른 회원 경로의 첨부 키는 거부한다")
    void create_rejects_foreign_attachment_key() {
        String foreign = "inquiries/" + UUID.randomUUID() + "/a.jpg";

        assertThatThrownBy(() -> inquiryService.create(MEMBER_ID, new InquiryCreateCommand(InquiryTopic.OTHER, "s",
                "c", List.of(new InquiryCreateCommand.Attachment(foreign, "image/jpeg")))))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_INVALID_ATTACHMENT_KEY);
        verify(inquiryRepository, never()).save(any());
    }

    @Test
    @DisplayName("내 문의 목록은 최신순 커서 페이징이다")
    void get_mine_paginates() {
        when(inquiryQueryRepository.findByMember(eq(MEMBER_ID), isNull(), isNull(), eq(2))).thenReturn(List.of(
                InquiryFixture.createWithId(UUID.randomUUID(), MEMBER_ID, CREATED_AT.plusDays(2)),
                InquiryFixture.createWithId(INQUIRY_ID, MEMBER_ID, CREATED_AT.plusDays(1))));

        CursorResponse<InquirySummary> result = inquiryService.getMine(MEMBER_ID, null, 1);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).startsWith(CREATED_AT.plusDays(2).toString() + ",");
    }

    @Test
    @DisplayName("문의 상세는 첨부 읽기 URL과 답변을 포함한다")
    void get_mine_detail_includes_attachment_urls_and_answer() {
        Inquiry inquiry = InquiryFixture.createWithAttachment(MEMBER_ID);
        inquiry.answer("Thanks!", UUID.randomUUID(), CREATED_AT.plusDays(1));
        when(inquiryRepository.findById(INQUIRY_ID)).thenReturn(Optional.of(inquiry));
        when(attachmentStorage.issueReadUrl(anyString())).thenReturn("https://read");

        InquiryDetail detail = inquiryService.getMine(MEMBER_ID, INQUIRY_ID);

        assertThat(detail.attachments()).hasSize(1);
        assertThat(detail.attachments().getFirst().url()).isEqualTo("https://read");
        assertThat(detail.answer().content()).isEqualTo("Thanks!");
    }

    @Test
    @DisplayName("남의 문의는 조회할 수 없다")
    void get_mine_detail_forbids_other_member() {
        when(inquiryRepository.findById(INQUIRY_ID)).thenReturn(Optional.of(InquiryFixture.create(UUID.randomUUID())));

        assertThatThrownBy(() -> inquiryService.getMine(MEMBER_ID, INQUIRY_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_FORBIDDEN);
        verify(attachmentStorage, never()).issueReadUrl(anyString());
    }

    @Test
    @DisplayName("스토리지가 꺼져 있으면 업로드 URL 발급이 503으로 실패한다")
    void issue_upload_url_fails_when_storage_unavailable() {
        when(attachmentStorage.issueUploadUrl(anyString(), anyString(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.IMAGE_UPLOAD_UNAVAILABLE));

        assertThatThrownBy(() -> inquiryService.issueUploadUrl(MEMBER_ID, "image/png", 100))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_UPLOAD_UNAVAILABLE);
    }
}
