package com.moodi.support.application.dto;

import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminInquiryDetail(UUID id, InquiryTopic topic, String subject, String content, InquiryStatus status,
                                 LocalDateTime createdAt, List<InquiryAttachmentView> attachments,
                                 AdminInquirySummary.Member member, Answer answer) {

    public record Answer(String content, UUID answeredBy, LocalDateTime answeredAt) {}
}
