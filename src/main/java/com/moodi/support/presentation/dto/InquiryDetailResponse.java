package com.moodi.support.presentation.dto;

import com.moodi.support.application.dto.InquiryAttachmentView;
import com.moodi.support.application.dto.InquiryDetail;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InquiryDetailResponse(UUID id, InquiryTopic topic, String subject, String content,
                                    InquiryStatus status, LocalDateTime createdAt,
                                    List<AttachmentResponse> attachments, AnswerResponse answer) {

    public record AttachmentResponse(String url, String contentType) {
        static AttachmentResponse from(InquiryAttachmentView view) {
            return new AttachmentResponse(view.url(), view.contentType());
        }
    }

    public record AnswerResponse(String content, LocalDateTime answeredAt) {}

    public static InquiryDetailResponse from(InquiryDetail detail) {
        return new InquiryDetailResponse(detail.id(), detail.topic(), detail.subject(), detail.content(),
                detail.status(), detail.createdAt(),
                detail.attachments().stream().map(AttachmentResponse::from).toList(),
                detail.answer() == null ? null
                        : new AnswerResponse(detail.answer().content(), detail.answer().answeredAt()));
    }
}
