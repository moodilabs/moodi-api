package com.moodi.spot.domain;

import java.util.List;
import java.util.Optional;

public interface SpotTranslationRepository {

    SpotTranslation save(SpotTranslation spotTranslation);

    Optional<SpotTranslation> findBySpotIdAndLocale(Long spotId, String locale);

    List<SpotTranslation> findBySpotIdIn(List<Long> spotIds);

    /**
     * 목록 응답용 제목 조회. locale 을 걸지 않으면 한 스팟에 여러 번역 행이 있을 때
     * 아무 행이나 뽑혀 검색 결과는 한국어 제목, 상세는 영문 제목이 나간다.
     */
    List<SpotTranslation> findBySpotIdInAndLocale(List<Long> spotIds, String locale);

    List<Long> findSpotIdsWithoutTranslation(String locale);
}
