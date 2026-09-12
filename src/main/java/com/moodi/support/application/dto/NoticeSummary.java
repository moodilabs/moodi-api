package com.moodi.support.application.dto;

import com.moodi.support.domain.Notice;
import com.moodi.support.domain.NoticeType;

import java.time.LocalDate;

public record NoticeSummary(Long id, NoticeType type, String title, String preview, LocalDate publishedAt) {

    public static NoticeSummary from(Notice notice) {
        return new NoticeSummary(notice.getId(), notice.getType(), notice.getTitle(), notice.preview(),
                notice.getPublishedAt());
    }
}
