package com.moodi.route.application;

import java.util.List;

/**
 * 기준 스팟과 무드가 비슷한 스팟을 찾는다 (RTE-F04).
 *
 * <p>Route 컨텍스트는 추천 알고리즘을 모른다. 이 포트를 통해
 * infrastructure 어댑터가 기존 무드 유사도 로직을 재활용한다.
 */
public interface SpotRecommendationReader {

    /**
     * 기준 스팟들의 무드 벡터와 유사한 스팟을 지역 내에서 찾는다.
     *
     * @param baseSpotIds 사용자가 선택한 기준 스팟 ID 목록
     * @param areas       탐색 지역 목록 (한국어 지역명)
     * @param limit       최대 추천 개수
     * @return 유사도 내림차순으로 정렬된 추천 스팟 스냅샷. 기준 스팟은 제외.
     */
    List<SpotSnapshot> recommend(List<Long> baseSpotIds, List<String> areas, int limit);
}
