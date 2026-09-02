package com.moodi.discovery.presentation.dto;

import com.moodi.discovery.application.AreaSuggestion;
import com.moodi.discovery.domain.PickAreaLevel;

/**
 * 지역 자동완성 결과 1건 (`DSC-04`).
 *
 * <p>{@code level}·{@code region}·{@code district}·{@code neighborhood}는 추천 요청의 지역 조건으로
 * 그대로 되돌려 보내는 값이다. {@code label}은 Chip 표기용으로 서버가 조립해 준다.
 */
public record AreaSuggestResponse(
        PickAreaLevel level,
        String region,
        String district,
        String neighborhood,
        String label
) {

    public static AreaSuggestResponse from(AreaSuggestion suggestion) {
        return new AreaSuggestResponse(suggestion.level(), suggestion.region(),
                suggestion.district(), suggestion.neighborhood(), suggestion.label());
    }
}
