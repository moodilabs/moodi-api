package com.moodi.support.presentation.dto.admin;

import com.moodi.support.application.dto.NoticeCommand;
import com.moodi.support.domain.NoticeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AdminNoticeRequest(
        @NotNull(message = "유형은 필수입니다.")
        NoticeType type,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "본문은 필수입니다.")
        @Size(max = 10_000, message = "본문은 10,000자 이하여야 합니다.")
        String content,

        boolean visible,

        /** 미지정 시 오늘. */
        LocalDate publishedAt
) {

    public NoticeCommand toCommand(LocalDate today) {
        return new NoticeCommand(type, title, content, visible, publishedAt == null ? today : publishedAt);
    }
}
