package com.moodi.discovery.application;

import com.moodi.discovery.domain.PickAreaLevel;

/**
 * 지역 자동완성 결과 1건 (`DSC-04`).
 *
 * <p>{@code region}·{@code district}·{@code neighborhood}는 클라이언트가 {@code POST /picks}의
 * 지역 조건으로 **그대로 되돌려 보내는 값**이다. 그래서 표기가 응답과 요청 사이에서 흔들리면 안 된다.
 * {@code label}은 화면 Chip에 그대로 쓰라고 서버가 조립해 준 문자열이다.
 */
public record AreaSuggestion(
        PickAreaLevel level,
        String region,
        String district,
        String neighborhood,
        String label
) {
}
