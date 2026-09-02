package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.AreaSuggestion;
import com.moodi.discovery.domain.PickAreaLevel;
import com.moodi.shared.support.PostgresTestSupport;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 원장은 한국어, 응답은 영문이라 표기 변환이 조회 한가운데 끼어 있다.
 * DISTINCT 집계와 한글 LIKE도 H2에서 그대로 재현되지 않아 Postgres에서 검증한다.
 */
class AreaSuggestReaderAdapterTest extends PostgresTestSupport {

    private static final int LIMIT = 20;

    @Autowired
    private EntityManager em;

    private AreaSuggestReaderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AreaSuggestReaderAdapter(em);
    }

    @Test
    @DisplayName("영문 시·도명으로 검색된다")
    void search_region_by_english_name() {
        insertPublishedSpot("서울", "성동구", "성수동");

        List<AreaSuggestion> suggestions = adapter.search("seoul", LIMIT);

        assertThat(suggestions).extracting(AreaSuggestion::level, AreaSuggestion::region)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(PickAreaLevel.REGION, "Seoul"));
    }

    @Test
    @DisplayName("영문 시·군·구명으로 검색되고 상위 지역이 함께 나온다")
    void search_district_by_english_name() {
        insertPublishedSpot("서울", "성동구", "성수동");

        List<AreaSuggestion> suggestions = adapter.search("seongdong", LIMIT);

        assertThat(suggestions).singleElement()
                .satisfies(suggestion -> {
                    assertThat(suggestion.level()).isEqualTo(PickAreaLevel.DISTRICT);
                    assertThat(suggestion.region()).isEqualTo("Seoul");
                    assertThat(suggestion.district()).isEqualTo("Seongdong-gu");
                    assertThat(suggestion.label()).isEqualTo("Seongdong-gu, Seoul");
                });
    }

    @Test
    @DisplayName("대소문자를 가리지 않는다")
    void search_is_case_insensitive() {
        insertPublishedSpot("부산", "해운대구", "우동");

        assertThat(adapter.search("BUSAN", LIMIT)).isNotEmpty();
        assertThat(adapter.search("busan", LIMIT)).isNotEmpty();
    }

    @Test
    @DisplayName("시·도가 시·군·구보다 앞에 나온다")
    void region_comes_before_district() {
        insertPublishedSpot("서울", "서초구", "서초동");

        List<AreaSuggestion> suggestions = adapter.search("seo", LIMIT);

        assertThat(suggestions).extracting(AreaSuggestion::level)
                .startsWith(PickAreaLevel.REGION);
        assertThat(suggestions).extracting(AreaSuggestion::level).contains(PickAreaLevel.DISTRICT);
    }

    @Test
    @DisplayName("스팟이 없는 지역은 후보에 나오지 않는다")
    void search_excludes_area_without_spot() {
        insertPublishedSpot("서울", "성동구", "성수동");

        assertThat(adapter.search("busan", LIMIT)).isEmpty();
    }

    @Test
    @DisplayName("비활성 스팟만 있는 지역은 후보에 나오지 않는다")
    void search_excludes_area_with_only_unpublished_spot() {
        insertUnpublishedSpot("부산", "해운대구", "우동");

        assertThat(adapter.search("busan", LIMIT)).isEmpty();
    }

    @Test
    @DisplayName("같은 지역의 스팟이 여러 개여도 후보는 하나로 합쳐진다")
    void search_deduplicates_areas() {
        insertPublishedSpot("서울", "성동구", "성수동");
        insertPublishedSpot("서울", "성동구", "금호동");
        insertPublishedSpot("서울", "성동구", "행당동");

        List<AreaSuggestion> suggestions = adapter.search("seongdong", LIMIT);

        assertThat(suggestions).hasSize(1);
    }

    @Test
    @DisplayName("동·면은 로마자로 옮겨 내보낸다")
    void search_neighborhood_returns_romanized_name() {
        insertPublishedSpot("서울", "성동구", "성수동");

        List<AreaSuggestion> suggestions = adapter.search("seongsu", LIMIT);

        assertThat(suggestions).singleElement()
                .satisfies(suggestion -> {
                    assertThat(suggestion.level()).isEqualTo(PickAreaLevel.NEIGHBORHOOD);
                    assertThat(suggestion.region()).isEqualTo("Seoul");
                    assertThat(suggestion.district()).isEqualTo("Seongdong-gu");
                    assertThat(suggestion.neighborhood()).isEqualTo("Seongsu");
                    assertThat(suggestion.label()).isEqualTo("Seongsu, Seoul");
                });
    }

    @Test
    @DisplayName("동·면은 한글로 입력해도 검색된다")
    void search_neighborhood_by_korean() {
        insertPublishedSpot("서울", "성동구", "성수동");

        List<AreaSuggestion> suggestions = adapter.search("성수", LIMIT);

        assertThat(suggestions).extracting(AreaSuggestion::neighborhood).containsExactly("Seongsu");
    }

    @Test
    @DisplayName("한글로 시·도명을 입력해도 검색된다")
    void search_region_by_korean_name() {
        insertPublishedSpot("서울", "성동구", "성수동");

        List<AreaSuggestion> suggestions = adapter.search("서울", LIMIT);

        assertThat(suggestions).isNotEmpty();
    }

    @Test
    @DisplayName("상한을 넘으면 상한만큼만 돌려준다")
    void search_respects_limit() {
        insertPublishedSpot("서울", "중구", "명동");
        insertPublishedSpot("부산", "중구", "남포동");
        insertPublishedSpot("대구", "중구", "동성로");

        List<AreaSuggestion> suggestions = adapter.search("jung", 2);

        assertThat(suggestions).hasSize(2);
    }

    @Test
    @DisplayName("일치하는 지역이 없으면 빈 목록이다")
    void search_with_no_match() {
        insertPublishedSpot("서울", "성동구", "성수동");

        assertThat(adapter.search("tokyo", LIMIT)).isEmpty();
    }

    private void insertPublishedSpot(String area, String district, String neighborhood) {
        Spot spot = createSpot(area, district, neighborhood);
        spot.publish();
        persist(spot);
    }

    private void insertUnpublishedSpot(String area, String district, String neighborhood) {
        persist(createSpot(area, district, neighborhood));
    }

    private Spot createSpot(String area, String district, String neighborhood) {
        return Spot.create("content-" + UUID.randomUUID(), SpotContentType.TOURIST_ATTRACTION,
                area, district, neighborhood, "kor_service", 126.0, 37.0, null, null, null, null, null);
    }

    private void persist(Spot spot) {
        em.persist(spot);
        em.flush();
    }
}
