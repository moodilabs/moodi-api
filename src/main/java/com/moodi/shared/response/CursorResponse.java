package com.moodi.shared.response;

import java.util.List;

public record CursorResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasNext
) {

    public static <T> CursorResponse<T> of(List<T> items, String nextCursor, boolean hasNext) {
        return new CursorResponse<>(items, nextCursor, hasNext);
    }

    public static <T> CursorResponse<T> empty() {
        return new CursorResponse<>(List.of(), null, false);
    }
}
