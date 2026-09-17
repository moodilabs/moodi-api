package com.moodi.route.presentation.dto;

import com.moodi.route.application.AreaCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RouteGenerateRequest(
        @NotNull(message = "스팟을 1개 이상 선택해야 합니다.")
        @Size(min = 1, max = 10, message = "스팟은 1~10개까지 선택 가능합니다.")
        List<Long> spotIds,

        @Size(max = 5, message = "지역은 최대 5개까지 선택 가능합니다.")
        @Valid
        List<AreaDto> areas,

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

    public List<AreaCondition> toAreaConditions() {
        return areas.stream().map(AreaDto::toCondition).toList();
    }

    /**
     * 지역 자동완성(`GET /api/v1/picks/areas`) 응답의 region·district를 그대로 되돌려 받는다.
     * neighborhood는 루트 생성 필터에서 쓰지 않아 받지 않는다 — {@link AreaCondition} 참고.
     */
    public record AreaDto(
            @NotBlank(message = "지역명은 필수입니다.") String region,
            String district
    ) {
        public AreaCondition toCondition() {
            return new AreaCondition(region, district);
        }
    }
}
