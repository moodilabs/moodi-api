package com.moodi.discovery.application;

import java.util.List;

/**
 * 피드 후보 조회 포트. 구현은 {@code discovery.infrastructure}의 어댑터가 맡고,
 * 이 인터페이스와 반환 타입은 다른 컨텍스트를 모른다.
 */
public interface FeedSpotReader {

    /**
     * 회원 피드 — 선호 무드 교집합 필터 + 노출 안 함 우선 + 무드 일치 수 + 시드 셔플.
     * 선호 무드가 비어 있으면 무드 필터 없이 노출 이력만으로 정렬한다.
     */
    List<FeedSpotRow> readPersonalizedFeed(FeedQuery query);

    /**
     * 비회원 피드 — 노출 이력·선호 무드가 없으므로 북마크 수 + 시드 셔플로 내려간다.
     */
    List<FeedSpotRow> readGuestFeed(FeedQuery query);
}
