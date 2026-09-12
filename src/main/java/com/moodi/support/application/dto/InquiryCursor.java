package com.moodi.support.application.dto;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.domain.Inquiry;

import java.time.LocalDateTime;
import java.util.UUID;

/** 문의 목록 커서. `{createdAt},{id}` (예: `2026-08-03T10:15:30,3f1c…`). */
public record InquiryCursor(LocalDateTime createdAt, UUID id) {

    public static InquiryCursor parse(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String[] parts = cursor.split(",", 2);
            return new InquiryCursor(LocalDateTime.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR_FORMAT);
        }
    }

    public static String of(Inquiry last) {
        return last.getCreatedAt() + "," + last.getId();
    }
}
