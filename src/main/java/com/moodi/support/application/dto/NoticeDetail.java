package com.moodi.support.application.dto;

import com.moodi.support.domain.Notice;
import com.moodi.support.domain.NoticeType;

import java.time.LocalDate;

public record NoticeDetail(Long id, NoticeType type, String title, String content, boolean visible,
                           LocalDate publishedAt) {

    public static NoticeDetail from(Notice notice) {
        return new NoticeDetail(notice.getId(), notice.getType(), notice.getTitle(), notice.getContent(),
                notice.isVisible(), notice.getPublishedAt());
    }
}
