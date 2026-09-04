package com.moodi.route.infrastructure.spot;

import com.moodi.discovery.domain.MoodSimilarity;
import com.moodi.route.application.SpotRecommendationReader;
import com.moodi.route.application.SpotSnapshot;
import com.moodi.route.domain.RouteSpotType;
import com.moodi.shared.mood.MoodVector;
import com.moodi.spot.application.RegionDictionary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 기준 스팟의 무드 벡터와 유사한 스팟을 지역 내에서 찾는다.
 *
 * <p>후보 선별(지역 필터 + 무드 벡터 존재)은 SQL에서, 유사도 정렬은 자바에서 한다.
 * Discovery의 {@code PickCandidateReaderAdapter}와 같은 전략이다.
 *
 * <p>컨텍스트 경계: {@code discovery.domain.MoodSimilarity}는 공유 커널({@code shared.mood})의
 * {@code MoodVector}만 참조한다. 성격상 shared로 옮겨야 하지만 합의 전이라
 * 지금은 discovery.domain을 직접 참조한다. 추후 이동 대상.
 */
@Component
@Transactional(readOnly = true)
public class SpotRecommendationReaderAdapter implements SpotRecommendationReader {

    private static final String DESCRIPTION_LOCALE = "en-US";
    private static final int CANDIDATE_LIMIT = 500;

    private static final String BASE_MOOD_SQL = """
            SELECT sm.mood_vector::text
            FROM spot_mood sm
            JOIN spot s ON s.id = sm.spot_id
            WHERE sm.spot_id IN (:baseSpotIds)
              AND s.status = 'PUBLISHED'
              AND sm.mood_vector IS NOT NULL
            """;

    private static final String CANDIDATE_SQL = """
            SELECT s.id, st.title, si.image_url, s.area, s.district,
                   s.latitude, s.longitude, s.content_type,
                   sd.content, sm.mood_vector::text
            FROM spot s
            JOIN spot_translation st ON st.spot_id = s.id AND st.locale = 'ko-KR'
            JOIN spot_mood sm ON sm.spot_id = s.id AND sm.mood_vector IS NOT NULL
            LEFT JOIN spot_description sd ON sd.spot_id = s.id AND sd.locale = :descLocale
            LEFT JOIN LATERAL (
                SELECT image_url FROM spot_image
                WHERE spot_id = s.id AND is_primary = true
                ORDER BY sort_order, id
                LIMIT 1
            ) si ON true
            WHERE s.status = 'PUBLISHED'
              AND s.route_excluded = false
              AND s.id NOT IN (:excludeIds)
            """;

    private final EntityManager em;

    public SpotRecommendationReaderAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<SpotSnapshot> recommend(List<Long> baseSpotIds, List<String> areas, int limit) {
        if (baseSpotIds.isEmpty()) {
            return List.of();
        }

        MoodVector baseMood = loadAverageMoodVector(baseSpotIds);
        if (baseMood == null) {
            return List.of();
        }

        List<CandidateRow> candidates = loadCandidates(baseSpotIds, areas);

        return candidates.stream()
                .filter(c -> c.moodVector != null)
                .sorted(Comparator.comparingDouble(
                        (CandidateRow c) -> MoodSimilarity.between(baseMood, c.moodVector)).reversed()
                        .thenComparingLong(c -> c.spotId))
                .limit(limit)
                .map(CandidateRow::toSnapshot)
                .toList();
    }

    private MoodVector loadAverageMoodVector(List<Long> baseSpotIds) {
        Query query = em.createNativeQuery(BASE_MOOD_SQL);
        query.setParameter("baseSpotIds", baseSpotIds);

        @SuppressWarnings("unchecked")
        List<String> rows = query.getResultList();

        List<MoodVector> vectors = rows.stream()
                .map(MoodVectorJsonReader::read)
                .filter(v -> v != null)
                .toList();

        if (vectors.isEmpty()) {
            return null;
        }
        if (vectors.size() == 1) {
            return vectors.get(0);
        }
        return MoodVectorAverager.average(vectors);
    }

    private List<CandidateRow> loadCandidates(List<Long> excludeIds, List<String> areas) {
        Map<String, Object> params = new HashMap<>();
        params.put("descLocale", DESCRIPTION_LOCALE);
        params.put("excludeIds", excludeIds);

        StringBuilder sql = new StringBuilder(CANDIDATE_SQL);
        if (areas != null && !areas.isEmpty()) {
            sql.append(areaFilter(areas, params));
        }
        sql.append("ORDER BY s.id\nLIMIT :candidateLimit");
        params.put("candidateLimit", CANDIDATE_LIMIT);

        Query query = em.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream().map(CandidateRow::from).toList();
    }

    private String areaFilter(List<String> areas, Map<String, Object> params) {
        List<String> conditions = new ArrayList<>();
        Set<String> koreanAreas = areas.stream()
                .map(RegionDictionary::toKoreanArea)
                .collect(Collectors.toSet());

        int i = 0;
        for (String area : koreanAreas) {
            conditions.add("s.area = :area" + i);
            params.put("area" + i, area);
            i++;
        }
        return "  AND (" + String.join(" OR ", conditions) + ")\n";
    }

    private record CandidateRow(
            long spotId, String title, String imageUrl,
            String area, String district,
            Double latitude, Double longitude,
            String contentType, String description,
            MoodVector moodVector
    ) {
        static CandidateRow from(Object[] row) {
            return new CandidateRow(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    row[5] == null ? null : ((Number) row[5]).doubleValue(),
                    row[6] == null ? null : ((Number) row[6]).doubleValue(),
                    (String) row[7],
                    (String) row[8],
                    MoodVectorJsonReader.read((String) row[9])
            );
        }

        SpotSnapshot toSnapshot() {
            return new SpotSnapshot(
                    spotId, title, imageUrl, area, district,
                    latitude, longitude,
                    RouteSpotType.valueOf(contentType),
                    description
            );
        }
    }
}
