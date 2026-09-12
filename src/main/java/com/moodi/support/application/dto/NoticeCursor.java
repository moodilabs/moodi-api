package com.moodi.support.application.dto;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.domain.Notice;

import java.time.LocalDate;

/**
 * 공지 목록 커서. `{publishedAt},{id}` 형식 (예: `2026-08-10,12`).
 */
public record NoticeCursor(LocalDate publishedAt, Long id) {

    public static NoticeCursor parse(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String[] parts = cursor.split(",", 2);
            return new NoticeCursor(LocalDate.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR_FORMAT);
        }
    }

    public static String of(Notice last) {
        return last.getPublishedAt() + "," + last.getId();
    }
}
