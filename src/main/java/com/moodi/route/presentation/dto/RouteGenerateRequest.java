package com.moodi.route.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RouteGenerateRequest(
        @NotNull(message = "스팟을 1개 이상 선택해야 합니다.")
        @Size(min = 1, max = 10, message = "스팟은 1~10개까지 선택 가능합니다.")
        List<Long> spotIds,

        @Size(max = 5, message = "지역은 최대 5개까지 선택 가능합니다.")
        List<String> areas,

        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate
) {
    public RouteGenerateRequest {
        if (areas == null) {
            areas = List.of();
        }
    }
}
