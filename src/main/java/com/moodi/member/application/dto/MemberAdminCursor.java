package com.moodi.member.application.dto;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.UUID;

/** 어드민 회원 목록 커서. `{createdAt},{id}`. */
public record MemberAdminCursor(LocalDateTime createdAt, UUID id) {

    public static MemberAdminCursor parse(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String[] parts = cursor.split(",", 2);
            return new MemberAdminCursor(LocalDateTime.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR_FORMAT);
        }
    }

    public static String of(MemberAdminRow last) {
        return last.createdAt() + "," + last.id();
    }
}
