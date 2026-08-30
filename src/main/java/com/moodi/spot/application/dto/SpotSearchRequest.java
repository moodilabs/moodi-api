package com.moodi.spot.application.dto;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;

import java.util.List;
import java.util.UUID;

public record SpotSearchRequest(
        String keyword,
        String area,
        List<String> moodTagKeys,
        boolean saved,
        SpotSearchSortType sort,
        UUID routePublicId,
        Long cursorSpotId,
        Long cursorBookmarkCount,
        Integer cursorMatchRank,
        int size
) {

    public static SpotSearchRequest of(String keyword, String area, List<String> moodTagKeys,
                                        boolean saved, SpotSearchSortType sort, UUID routePublicId,
                                        String cursor, int size) {
        Long cursorSpotId = null;
        Long cursorBookmarkCount = null;
        Integer cursorMatchRank = null;

        if (cursor != null && !cursor.isBlank()) {
            try {
                String[] parts = cursor.split(",");
                if (parts.length == 3) {
                    cursorMatchRank = Integer.parseInt(parts[0]);
                    cursorBookmarkCount = Long.parseLong(parts[1]);
                    cursorSpotId = Long.parseLong(parts[2]);
                } else {
                    cursorBookmarkCount = Long.parseLong(parts[0]);
                    cursorSpotId = Long.parseLong(parts[1]);
                }
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }

        return new SpotSearchRequest(keyword, area, moodTagKeys, saved, sort, routePublicId,
                cursorSpotId, cursorBookmarkCount, cursorMatchRank, size);
    }
}
