package com.moodi.discovery.application;

import java.util.List;
import java.util.UUID;

/**
 * @param analysisType  추천 방식 — {@code "AI"}(무드 분석 기반) 또는 {@code "POPULAR"}(인기순 fallback)
 * @param spots         선택 지역 안에서 뽑은 추천 결과 (DSC-05-01)
 * @param fallbackSpots spots가 비었을 때만 채워지는 대체 추천 (DSC-05-02)
 */
public record PickResult(UUID pickId, String analysisType, List<PickResultItem> spots, List<PickResultItem> fallbackSpots) {
}
