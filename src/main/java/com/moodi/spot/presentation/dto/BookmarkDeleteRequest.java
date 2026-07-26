package com.moodi.spot.presentation.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BookmarkDeleteRequest(
        @NotEmpty(message = "삭제할 스팟 ID 목록은 비어 있을 수 없습니다.")
        List<Long> spotIds
) {
}
