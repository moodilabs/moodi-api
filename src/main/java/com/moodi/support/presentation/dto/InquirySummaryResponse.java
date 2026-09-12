package com.moodi.support.presentation.dto;

import com.moodi.support.application.dto.InquirySummary;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;

import java.time.LocalDateTime;
import java.util.UUID;

public record InquirySummaryResponse(UUID id, InquiryTopic topic, String subject, String preview,
                                     InquiryStatus status, LocalDateTime createdAt) {

    public static InquirySummaryResponse from(InquirySummary summary) {
        return new InquirySummaryResponse(summary.id(), summary.topic(), summary.subject(), summary.preview(),
                summary.status(), summary.createdAt());
    }
}
