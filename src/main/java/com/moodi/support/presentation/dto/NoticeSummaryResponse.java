package com.moodi.support.presentation.dto;

import com.moodi.support.application.dto.NoticeSummary;
import com.moodi.support.domain.NoticeType;

import java.time.LocalDate;

public record NoticeSummaryResponse(Long id, NoticeType type, String title, String preview, LocalDate publishedAt) {

    public static NoticeSummaryResponse from(NoticeSummary summary) {
        return new NoticeSummaryResponse(summary.id(), summary.type(), summary.title(), summary.preview(),
                summary.publishedAt());
    }
}
