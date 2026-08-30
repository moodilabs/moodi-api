package com.moodi.discovery.presentation.dto;

import com.moodi.discovery.domain.PickArea;
import com.moodi.discovery.domain.PickAreaLevel;
import com.moodi.discovery.domain.PickAreas;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PickRequestDto(
        @NotBlank String imageKey,
        @NotEmpty @Size(max = PickAreas.MAX_SIZE) @Valid List<AreaDto> areas
) {

    public List<PickArea> toAreas() {
        return areas.stream().map(AreaDto::toArea).toList();
    }

    public record AreaDto(
            @NotNull PickAreaLevel level,
            @NotBlank String region,
            String district,
            String neighborhood
    ) {

        public PickArea toArea() {
            return new PickArea(level, region, district, neighborhood);
        }
    }
}
