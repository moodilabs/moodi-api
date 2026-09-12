package com.moodi.support.presentation.dto;

import com.moodi.support.application.dto.InquiryCreateCommand;
import com.moodi.support.domain.InquiryTopic;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InquiryCreateRequest(
        @NotNull(message = "주제는 필수입니다.")
        InquiryTopic topic,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 60, message = "제목은 60자 이하여야 합니다.")
        String subject,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 1000, message = "내용은 1,000자 이하여야 합니다.")
        String content,

        @Size(max = 5, message = "첨부는 5개까지 가능합니다.")
        List<@Valid Attachment> attachments
) {

    public record Attachment(
            @NotBlank(message = "첨부 키는 필수입니다.")
            String attachmentKey,

            @NotBlank(message = "첨부 형식은 필수입니다.")
            String contentType
    ) {
    }

    public InquiryCreateCommand toCommand() {
        List<InquiryCreateCommand.Attachment> mapped = attachments == null ? List.of()
                : attachments.stream()
                .map(attachment -> new InquiryCreateCommand.Attachment(attachment.attachmentKey(),
                        attachment.contentType()))
                .toList();
        return new InquiryCreateCommand(topic, subject, content, mapped);
    }
}
