package com.moodi.support.presentation.dto;

import com.moodi.support.application.dto.NoticeDetail;
import com.moodi.support.domain.NoticeType;

import java.time.LocalDate;

public record NoticeDetailResponse(Long id, NoticeType type, String title, String content, LocalDate publishedAt) {

    public static NoticeDetailResponse from(NoticeDetail detail) {
        return new NoticeDetailResponse(detail.id(), detail.type(), detail.title(), detail.content(),
                detail.publishedAt());
    }
}
