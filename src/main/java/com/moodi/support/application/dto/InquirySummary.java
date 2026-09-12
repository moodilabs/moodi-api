package com.moodi.support.application.dto;

import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;

import java.time.LocalDateTime;
import java.util.UUID;

public record InquirySummary(UUID id, InquiryTopic topic, String subject, String preview, InquiryStatus status,
                             LocalDateTime createdAt) {

    private static final int PREVIEW_LENGTH = 100;

    public static InquirySummary from(Inquiry inquiry) {
        String flattened = inquiry.getContent().replaceAll("\\s+", " ").trim();
        String preview = flattened.length() <= PREVIEW_LENGTH ? flattened : flattened.substring(0, PREVIEW_LENGTH);
        return new InquirySummary(inquiry.getId(), inquiry.getTopic(), inquiry.getSubject(), preview,
                inquiry.getStatus(), inquiry.getCreatedAt());
    }
}
