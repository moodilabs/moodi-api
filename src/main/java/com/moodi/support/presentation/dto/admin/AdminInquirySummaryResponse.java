package com.moodi.support.presentation.dto.admin;

import com.moodi.support.application.dto.AdminInquirySummary;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminInquirySummaryResponse(UUID id, InquiryTopic topic, String subject, InquiryStatus status,
                                          LocalDateTime createdAt, LocalDateTime answeredAt,
                                          MemberResponse member) {

    public record MemberResponse(UUID id, String nickname, String email, boolean withdrawn) {
        static MemberResponse from(AdminInquirySummary.Member member) {
            return member == null ? null
                    : new MemberResponse(member.id(), member.nickname(), member.email(), member.withdrawn());
        }
    }

    public static AdminInquirySummaryResponse from(AdminInquirySummary summary) {
        return new AdminInquirySummaryResponse(summary.id(), summary.topic(), summary.subject(), summary.status(),
                summary.createdAt(), summary.answeredAt(), MemberResponse.from(summary.member()));
    }
}
