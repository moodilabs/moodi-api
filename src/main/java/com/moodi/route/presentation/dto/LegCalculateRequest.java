package com.moodi.route.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LegCalculateRequest(
        @NotNull(message = "구간 목록은 필수입니다.")
        @Size(min = 1, max = 20, message = "구간은 1~20개까지 요청 가능합니다.")
        @Valid
        List<@NotNull(message = "구간 항목은 null일 수 없습니다.") SpotPair> pairs
) {
    public record SpotPair(
            @NotNull(message = "출발 스팟 ID는 필수입니다.")
            Long fromSpotId,

            @NotNull(message = "도착 스팟 ID는 필수입니다.")
            Long toSpotId
    ) {}
}
