package com.moodi.route.application;

/**
 * 루트 생성 시 추가 스팟을 찾을 지역 조건.
 *
 * <p>{@code district}가 없으면 시·도 전체가 대상이다. Pick(DSC-04)과 달리 루트는 여러 날에 걸쳐
 * 스팟을 채우는 용도라 동/면 단위까지는 구분하지 않는다 — 자동완성이 동 단위 결과를 내려주더라도
 * 그 상위 구/군으로 필터링한다.
 */
public record AreaCondition(String region, String district) {
}
