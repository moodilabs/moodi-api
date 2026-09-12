package com.moodi.support.presentation.dto.admin;

import com.moodi.support.application.dto.AdminInquiryDetail;
import com.moodi.support.application.dto.InquiryAttachmentView;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminInquiryDetailResponse(UUID id, InquiryTopic topic, String subject, String content,
                                         InquiryStatus status, LocalDateTime createdAt,
                                         List<AttachmentResponse> attachments,
                                         AdminInquirySummaryResponse.MemberResponse member,
                                         AnswerResponse answer) {

    public record AttachmentResponse(String url, String contentType) {
        static AttachmentResponse from(InquiryAttachmentView view) {
            return new AttachmentResponse(view.url(), view.contentType());
        }
    }

    public record AnswerResponse(String content, UUID answeredBy, LocalDateTime answeredAt) {}

    public static AdminInquiryDetailResponse from(AdminInquiryDetail detail) {
        return new AdminInquiryDetailResponse(detail.id(), detail.topic(), detail.subject(), detail.content(),
                detail.status(), detail.createdAt(),
                detail.attachments().stream().map(AttachmentResponse::from).toList(),
                detail.member() == null ? null : new AdminInquirySummaryResponse.MemberResponse(
                        detail.member().id(), detail.member().nickname(), detail.member().email(),
                        detail.member().withdrawn()),
                detail.answer() == null ? null : new AnswerResponse(detail.answer().content(),
                        detail.answer().answeredBy(), detail.answer().answeredAt()));
    }
}
