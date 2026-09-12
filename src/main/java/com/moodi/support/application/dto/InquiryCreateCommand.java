package com.moodi.support.application.dto;

import com.moodi.support.domain.InquiryTopic;

import java.util.List;

/** @param attachments 업로드 URL 발급 때 받은 키와 형식. 순서대로 저장된다. */
public record InquiryCreateCommand(InquiryTopic topic, String subject, String content,
                                   List<Attachment> attachments) {

    public record Attachment(String attachmentKey, String contentType) {}
}
