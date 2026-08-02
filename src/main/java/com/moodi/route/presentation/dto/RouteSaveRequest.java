package com.moodi.route.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RouteSaveRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 40, message = "제목은 최대 40자까지 가능합니다.")
        String title,

        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate,

        @NotNull(message = "일자 정보는 필수입니다.")
        @Size(min = 1, message = "최소 1일 이상의 일정이 필요합니다.")
        @Valid
        List<@NotNull(message = "일자 항목은 null일 수 없습니다.") DayRequest> days
) {
    public record DayRequest(
            @NotNull(message = "일차 번호는 필수입니다.")
            Integer dayNumber,

            @NotNull(message = "날짜는 필수입니다.")
            LocalDate date,

            @NotNull(message = "스팟 목록은 필수입니다.")
            List<@NotNull(message = "스팟 ID는 null일 수 없습니다.") Long> spotIds
    ) {
    }
}
