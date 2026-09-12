package com.moodi.support.presentation.dto.admin;

import com.moodi.support.application.dto.NoticeDetail;
import com.moodi.support.domain.NoticeType;

import java.time.LocalDate;

public record AdminNoticeResponse(Long id, NoticeType type, String title, String content, boolean visible,
                                  LocalDate publishedAt) {

    public static AdminNoticeResponse from(NoticeDetail detail) {
        return new AdminNoticeResponse(detail.id(), detail.type(), detail.title(), detail.content(),
                detail.visible(), detail.publishedAt());
    }
}
