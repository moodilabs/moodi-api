package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.PickCandidate;
import com.moodi.discovery.application.PickCandidateReader;
import com.moodi.discovery.domain.PickArea;
import com.moodi.discovery.domain.PickAreaLevel;
import com.moodi.discovery.domain.PickAreas;
import com.moodi.discovery.infrastructure.region.RegionNames;
import com.moodi.shared.mood.MoodTag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 추천 후보를 읽는다. 유사도 정렬은 서비스에서 하므로 여기서는 후보 선별만 한다.
 *
 * <p>무드 태그 조건은 {@code ?|}가 아니라 {@code @>} OR 체인으로 짠다. Hibernate 네이티브 쿼리는
 * 파라미터를 직접 파싱하면서 {@code ?}를 위치 파라미터로 인식해 {@code ?|}가 실패한다(이슈 #38).
 * {@code @>}는 기존 {@code idx_spot_mood_tags}(GIN, jsonb_path_ops)가 지원하는 연산자이기도 하다.
 */
@Repository
public class PickCandidateReaderAdapter implements PickCandidateReader {

    private static final String DEFAULT_LOCALE = "ko-KR";

    /*
     * 지역명은 원장에 한국어로 저장돼 있고 응답은 영문으로 나간다. 클라이언트는 자동완성에서 받은
     * 영문 지역명을 그대로 되돌려 주므로, 조회 조건으로 쓰기 전에 한국어로 되돌린다.
     * 동(neighborhood)은 사전에 없어 한국어 그대로 비교한다.
     */

    private static final String SELECT = """
            SELECT s.id, st.title, si.image_url, s.area, s.district, s.neighborhood,
                   st.addr1, sd.content, s.latitude, s.longitude,
                   sm.mood_tags::text, sm.mood_vector::text, (bm.spot_id IS NOT NULL)
            FROM spot s
            JOIN spot_translation st ON st.spot_id = s.id AND st.locale = :locale
            JOIN spot_mood sm ON sm.spot_id = s.id
            LEFT JOIN spot_description sd ON sd.spot_id = s.id AND sd.locale = :locale
            LEFT JOIN LATERAL (
                SELECT image_url FROM spot_image
                WHERE spot_id = s.id AND is_primary = true
                ORDER BY sort_order, id
                LIMIT 1
            ) si ON true
            LEFT JOIN bookmark bm ON bm.spot_id = s.id AND bm.member_id = :memberId
            WHERE s.status = 'PUBLISHED'
            """;

    private final EntityManager em;

    public PickCandidateReaderAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<PickCandidate> readByAreas(UUID memberId, PickAreas areas, int limit) {
        Map<String, Object> params = baseParams(memberId, limit);
        String sql = SELECT + areaFilter(areas, params) + "ORDER BY s.id\nLIMIT :limit";
        return execute(sql, params);
    }

    @Override
    public List<PickCandidate> readByMoodTags(UUID memberId, List<MoodTag> moodTags, int limit) {
        if (moodTags.isEmpty()) {
            return List.of();
        }
        Map<String, Object> params = baseParams(memberId, limit);
        String sql = SELECT + moodTagFilter(moodTags, params) + "ORDER BY s.id\nLIMIT :limit";
        return execute(sql, params);
    }

    private Map<String, Object> baseParams(UUID memberId, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("locale", DEFAULT_LOCALE);
        params.put("memberId", memberId);
        params.put("limit", limit);
        return params;
    }

    /**
     * 선택 지역 중 하나라도 걸리면 후보다. 단계별로 비교 깊이가 달라 조건을 따로 만든다.
     */
    private String areaFilter(PickAreas areas, Map<String, Object> params) {
        List<String> conditions = new ArrayList<>();
        List<PickArea> values = areas.values();
        for (int i = 0; i < values.size(); i++) {
            PickArea area = values.get(i);
            StringBuilder condition = new StringBuilder("(s.area = :region" + i);
            params.put("region" + i, RegionNames.toKoreanArea(area.region()));
            if (area.level() != PickAreaLevel.REGION) {
                condition.append(" AND s.district = :district").append(i);
                params.put("district" + i, RegionNames.toKoreanDistrict(area.district()));
            }
            if (area.level() == PickAreaLevel.NEIGHBORHOOD) {
                condition.append(" AND s.neighborhood = :neighborhood").append(i);
                params.put("neighborhood" + i, area.neighborhood());
            }
            conditions.add(condition.append(")").toString());
        }
        return "  AND (" + String.join(" OR ", conditions) + ")\n";
    }

    private String moodTagFilter(List<MoodTag> moodTags, Map<String, Object> params) {
        List<String> conditions = new ArrayList<>();
        for (int i = 0; i < moodTags.size(); i++) {
            conditions.add("sm.mood_tags @> CAST(:moodTag" + i + " AS jsonb)");
            params.put("moodTag" + i, "[\"" + moodTags.get(i).getKey() + "\"]");
        }
        return "  AND (" + String.join(" OR ", conditions) + ")\n";
    }

    private List<PickCandidate> execute(String sql, Map<String, Object> params) {
        Query query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(row -> new PickCandidate(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        RegionNames.toEnglishArea((String) row[3]),
                        (String) row[4],
                        (String) row[5],
                        (String) row[6],
                        (String) row[7],
                        row[8] == null ? null : ((Number) row[8]).doubleValue(),
                        row[9] == null ? null : ((Number) row[9]).doubleValue(),
                        MoodTagJsonReader.read((String) row[10]),
                        MoodVectorJsonReader.read((String) row[11]),
                        (Boolean) row[12]
                ))
                .toList();
    }
}
