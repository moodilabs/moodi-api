package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.AreaSuggestReader;
import com.moodi.discovery.application.AreaSuggestion;
import com.moodi.discovery.domain.PickAreaLevel;
import com.moodi.discovery.infrastructure.region.RegionNames;
import com.moodi.discovery.infrastructure.region.Romanizer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 지역 자동완성 후보를 스팟 원장에서 읽는다 (`DSC-04`).
 *
 * <p><b>시·도와 시·군·구는 메모리에서 거른다.</b> 원장은 지역을 한국어로 저장하는데 사용자는 영문으로
 * 입력하므로, SQL {@code LIKE}로는 매칭할 수 없다. 사전이 접두어 검색을 지원하지 않아
 * 키워드를 한국어로 미리 바꿀 수도 없다. 그래서 원장에 실재하는 조합만 뽑아 영문으로 바꾼 뒤 거른다.
 * {@code (area, district)}는 조합 수가 시·도 20개 × 시·군·구 209개로 상한이 낮고
 * {@code idx_spot_area_district}가 그대로 덮어 인덱스만 훑고 끝난다.
 *
 * <p><b>동·면은 사전이 없어 {@link Romanizer}로 옮긴다.</b> 수천 개라 손으로 사전을 만들 수 없고,
 * 국어의 로마자 표기법이 규칙으로 정의돼 있어 변환으로 대체한다. 다만 변환은 되돌릴 수 없으므로
 * <b>후보를 읽어 옮긴 뒤 비교한다.</b> 원문도 함께 봐서 한글 입력도 걸린다.
 */
@Repository
public class AreaSuggestReaderAdapter implements AreaSuggestReader {

    private static final String REGION_DISTRICT_SQL = """
            SELECT DISTINCT area, district
            FROM spot
            WHERE status = 'PUBLISHED'
            """;

    private static final String NEIGHBORHOOD_SQL = """
            SELECT DISTINCT area, district, neighborhood
            FROM spot
            WHERE status = 'PUBLISHED'
              AND neighborhood IS NOT NULL
              AND district IS NOT NULL
            ORDER BY area, district, neighborhood
            """;

    private final EntityManager em;

    public AreaSuggestReaderAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<AreaSuggestion> search(String keyword, int limit) {
        String normalized = keyword.toLowerCase(Locale.ROOT);

        List<AreaSuggestion> suggestions = new ArrayList<>(searchRegionsAndDistricts(normalized, limit));
        if (suggestions.size() < limit) {
            suggestions.addAll(searchNeighborhoods(normalized, limit - suggestions.size()));
        }
        return suggestions;
    }

    /**
     * 시·도가 시·군·구보다 앞에 온다. 넓은 범위를 먼저 보여주는 편이 고르기 쉽고,
     * 상위를 고르면 하위는 어차피 포함되기 때문이다.
     */
    private List<AreaSuggestion> searchRegionsAndDistricts(String normalized, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(REGION_DISTRICT_SQL).getResultList();

        Set<AreaSuggestion> regions = new LinkedHashSet<>();
        Set<AreaSuggestion> districts = new LinkedHashSet<>();
        for (Object[] row : rows) {
            String koreanRegion = (String) row[0];
            String region = RegionNames.toEnglishArea(koreanRegion);
            if (matches(normalized, region, koreanRegion)) {
                regions.add(new AreaSuggestion(PickAreaLevel.REGION, region, null, null, region));
            }
            String koreanDistrict = (String) row[1];
            if (koreanDistrict == null) {
                continue;
            }
            String district = RegionNames.toEnglishDistrict(koreanDistrict);
            if (matches(normalized, district, koreanDistrict)) {
                districts.add(new AreaSuggestion(PickAreaLevel.DISTRICT, region, district, null,
                        label(district, region)));
            }
        }

        List<AreaSuggestion> merged = new ArrayList<>(regions);
        merged.addAll(districts);
        return merged.size() > limit ? merged.subList(0, limit) : merged;
    }

    /**
     * 로마자 변환은 되돌릴 수 없어 키워드를 한글로 바꿔 SQL에 넘길 수 없다.
     * 그래서 후보를 읽어 옮긴 뒤 비교한다. 한글 입력도 같은 자리에서 원문으로 함께 본다.
     */
    private List<AreaSuggestion> searchNeighborhoods(String normalized, int limit) {
        Query query = em.createNativeQuery(NEIGHBORHOOD_SQL);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<AreaSuggestion> suggestions = new ArrayList<>();
        for (Object[] row : rows) {
            if (suggestions.size() >= limit) {
                break;
            }
            String koreanNeighborhood = (String) row[2];
            String neighborhood = Romanizer.romanizePlaceName(koreanNeighborhood);
            if (!matches(normalized, neighborhood, koreanNeighborhood)) {
                continue;
            }
            String region = RegionNames.toEnglishArea((String) row[0]);
            suggestions.add(new AreaSuggestion(PickAreaLevel.NEIGHBORHOOD, region,
                    RegionNames.toEnglishDistrict((String) row[1]), neighborhood,
                    label(neighborhood, region)));
        }
        return suggestions;
    }

    /**
     * 하위 단계 Chip은 상위 지역을 함께 보여준다. 같은 이름의 구·동이 여러 시·도에 있어
     * 이름만으로는 어디인지 알 수 없기 때문이다(예: `Jung-gu`).
     */
    private String label(String name, String region) {
        return name + ", " + region;
    }

    /**
     * 영문·한국어 어느 쪽으로 입력해도 걸리게 한다. 응답은 영문이지만 한글로 찾는 사용자도 있고,
     * 세 단계가 같은 필드를 쓰므로 입력 언어에 따라 결과가 갈리면 안 된다.
     */
    private boolean matches(String normalized, String englishName, String koreanName) {
        return (englishName != null && englishName.toLowerCase(Locale.ROOT).contains(normalized))
                || (koreanName != null && koreanName.contains(normalized));
    }
}
