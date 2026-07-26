package com.moodi.spot.application.dto;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

import java.time.LocalDateTime;
import java.util.List;

public record BookmarkListRequest(
        String area,
        List<String> moodTagKeys,
        BookmarkSortType sort,
        Long cursorId,
        LocalDateTime cursorCreatedAt,
        Long cursorSpotId,
        Long cursorBookmarkCount,
        int size
) {

    public static BookmarkListRequest of(String area, List<String> moodTagKeys, BookmarkSortType sort,
                                          String cursor, int size) {
        Long cursorId = null;
        LocalDateTime cursorCreatedAt = null;
        Long cursorSpotId = null;
        Long cursorBookmarkCount = null;

        if (cursor != null && !cursor.isBlank()) {
            try {
                String[] parts = cursor.split(",", 2);
                if (sort == BookmarkSortType.POPULAR) {
                    cursorBookmarkCount = Long.parseLong(parts[0]);
                    cursorSpotId = Long.parseLong(parts[1]);
                } else {
                    cursorCreatedAt = LocalDateTime.parse(parts[0]);
                    cursorId = Long.parseLong(parts[1]);
                }
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }

        return new BookmarkListRequest(area, moodTagKeys, sort, cursorId, cursorCreatedAt,
                cursorSpotId, cursorBookmarkCount, size);
    }
}
