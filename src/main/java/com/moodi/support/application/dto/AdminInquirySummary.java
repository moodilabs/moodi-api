package com.moodi.support.application.dto;

import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminInquirySummary(UUID id, InquiryTopic topic, String subject, InquiryStatus status,
                                  LocalDateTime createdAt, LocalDateTime answeredAt, Member member) {

    /** 탈퇴 회원은 nickname·email이 null이고 withdrawn이 true. 회원 행 자체가 없으면(비정상) member가 null. */
    public record Member(UUID id, String nickname, String email, boolean withdrawn) {}
}
