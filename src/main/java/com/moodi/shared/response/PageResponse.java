package com.moodi.shared.response;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static <T> PageResponse<T> of(List<T> data, int page, int size, long totalElements) {
        return new PageResponse<>(
                data,
                page,
                size,
                totalElements,
                (int) ((totalElements + size - 1) / size),
                (long) (page + 1) * size < totalElements
        );
    }
}
