package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.FeedCursor;
import com.moodi.discovery.application.FeedQuery;
import com.moodi.discovery.application.FeedSpotReader;
import com.moodi.discovery.application.FeedSpotRow;
import com.moodi.discovery.infrastructure.region.RegionNames;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 피드 후보를 네이티브 쿼리 하나로 조회한다. 목록에 필요한 필드가 spot·spot_translation·spot_image에
 * 흩어져 있고 정렬 키가 SQL 표현식이라, 후보 선별·정렬·페이징을 CTE로 묶고 마지막에 한 페이지분의
 * 상세만 붙이는 편이 페이지당 왕복과 정렬 비용을 함께 줄인다.
 *
 * <p>무드 교집합은 {@code ?|}가 아니라 {@code @>} OR 체인으로 짠다. Hibernate 네이티브 쿼리는
 * SQL을 JDBC에 넘기기 전에 파라미터를 직접 파싱하는데, {@code ?}를 JDBC 위치 파라미터로 인식해
 * {@code ?|}·{@code ??|} 모두 ParameterRecognitionException으로 실패한다. {@code @>}는
 * 기존 {@code idx_spot_mood_tags}(GIN, jsonb_path_ops)가 지원하는 연산자이기도 하다.
 */
@Repository
public class FeedSpotReaderAdapter implements FeedSpotReader {

    /**
     * 서비스 언어는 영문이다(스토어 등록정보·앱 UI 모두 en-US).
     * `spot` 원장은 한국어로 적재되고 `spot_translation`·`spot_description`에 en-US가 함께 쌓이므로,
     * 사용자에게 나가는 조회는 en-US를 본다. 스팟 상세·검색·북마크도 같은 로케일을 쓴다.
     */
    private static final String DEFAULT_LOCALE = "en-US";

    /**
     * 대표 이미지는 정렬·페이징이 끝난 뒤 한 페이지분에만 붙인다. 후보 전건에 붙이면
     * 5만 건 카탈로그에서 LATERAL 조인이 그만큼 돌고, image_url이 정렬 페이로드에 실려
     * 정렬이 디스크로 넘어간다.
     */
    private static final String PAGE_PROJECTION = """
            SELECT p.spot_id, p.title, si.image_url, p.area, p.bookmarked, p.seen, p.rank_score, p.shuffle_key
            FROM page p
            LEFT JOIN LATERAL (
                SELECT image_url FROM spot_image
                WHERE spot_id = p.spot_id AND is_primary = true
                ORDER BY sort_order, id
                LIMIT 1
            ) si ON true
            """;

    private final EntityManager em;

    public FeedSpotReaderAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<FeedSpotRow> readPersonalizedFeed(FeedQuery query) {
        return execute(personalizedFeedSql(query));
    }

    @Override
    public List<FeedSpotRow> readGuestFeed(FeedQuery query) {
        return execute(guestFeedSql(query));
    }

    /**
     * 실행 계획 검증에서 실제로 나가는 SQL을 그대로 EXPLAIN하려고 조립과 실행을 분리해 둔다.
     */
    PreparedSql personalizedFeedSql(FeedQuery query) {
        Map<String, Object> params = new HashMap<>();
        params.put("locale", DEFAULT_LOCALE);
        params.put("memberId", query.memberId());
        params.put("seed", query.seed());
        params.put("sessionAt", query.sessionAt());
        params.put("impressionFrom", query.impressionFrom());
        params.put("limit", query.limit());

        String ordering = "ORDER BY seen ASC, rank_score DESC, shuffle_key ASC, spot_id ASC\n";
        String sql = "WITH candidate AS (\n"
                + "SELECT s.id AS spot_id, st.title AS title, s.area AS area,\n"
                + "       CASE WHEN fi.spot_id IS NULL THEN 0 ELSE 1 END AS seen,\n"
                + "       " + matchCountExpression(query.moodTagKeys(), params) + " AS rank_score,\n"
                + "       md5(s.id::text || :seed) AS shuffle_key,\n"
                + "       (bm.spot_id IS NOT NULL) AS bookmarked\n"
                + "FROM spot s\n"
                + "JOIN spot_translation st ON st.spot_id = s.id AND st.locale = :locale\n"
                + "LEFT JOIN spot_mood sm ON sm.spot_id = s.id\n"
                + "LEFT JOIN feed_impression fi ON fi.spot_id = s.id\n"
                + "     AND fi.member_id = :memberId\n"
                + "     AND fi.shown_at > :impressionFrom\n"
                + "     AND fi.shown_at <= :sessionAt\n"
                + "LEFT JOIN bookmark bm ON bm.spot_id = s.id AND bm.member_id = :memberId\n"
                + "WHERE s.status = 'PUBLISHED'\n"
                + moodFilter(query.moodTagKeys())
                + "), page AS (\n"
                + "SELECT * FROM candidate\n"
                + personalizedCursorPredicate(query.cursor(), params)
                + ordering
                + "LIMIT :limit\n"
                + ")\n"
                + PAGE_PROJECTION
                + "ORDER BY p.seen ASC, p.rank_score DESC, p.shuffle_key ASC, p.spot_id ASC";

        return new PreparedSql(sql, params);
    }

    PreparedSql guestFeedSql(FeedQuery query) {
        Map<String, Object> params = new HashMap<>();
        params.put("locale", DEFAULT_LOCALE);
        params.put("seed", query.seed());
        params.put("limit", query.limit());

        String ordering = "ORDER BY rank_score DESC, shuffle_key ASC, spot_id ASC\n";
        String sql = "WITH candidate AS (\n"
                + "SELECT s.id AS spot_id, st.title AS title, s.area AS area,\n"
                + "       0 AS seen,\n"
                + "       COALESCE(bc.bookmark_count, 0) AS rank_score,\n"
                + "       md5(s.id::text || :seed) AS shuffle_key,\n"
                + "       false AS bookmarked\n"
                + "FROM spot s\n"
                + "JOIN spot_translation st ON st.spot_id = s.id AND st.locale = :locale\n"
                + "LEFT JOIN (\n"
                + "    SELECT spot_id, COUNT(*) AS bookmark_count FROM bookmark GROUP BY spot_id\n"
                + ") bc ON bc.spot_id = s.id\n"
                + "WHERE s.status = 'PUBLISHED'\n"
                + "), page AS (\n"
                + "SELECT * FROM candidate\n"
                + guestCursorPredicate(query.cursor(), params)
                + ordering
                + "LIMIT :limit\n"
                + ")\n"
                + PAGE_PROJECTION
                + "ORDER BY p.rank_score DESC, p.shuffle_key ASC, p.spot_id ASC";

        return new PreparedSql(sql, params);
    }

    /**
     * 선호 무드가 없으면 일치 수를 따질 대상이 없으므로 0으로 고정하고, 정렬은 노출 이력과 셔플만 쓴다.
     */
    private String matchCountExpression(List<String> moodTagKeys, Map<String, Object> params) {
        if (moodTagKeys.isEmpty()) {
            return "0";
        }
        StringBuilder expression = new StringBuilder("(");
        for (int i = 0; i < moodTagKeys.size(); i++) {
            if (i > 0) {
                expression.append(" + ");
            }
            expression.append("CASE WHEN sm.mood_tags @> CAST(:moodTag").append(i)
                    .append(" AS jsonb) THEN 1 ELSE 0 END");
            params.put("moodTag" + i, toJsonArray(moodTagKeys.get(i)));
        }
        return expression.append(")").toString();
    }

    private String moodFilter(List<String> moodTagKeys) {
        if (moodTagKeys.isEmpty()) {
            return "";
        }
        StringBuilder filter = new StringBuilder("  AND (");
        for (int i = 0; i < moodTagKeys.size(); i++) {
            if (i > 0) {
                filter.append(" OR ");
            }
            filter.append("sm.mood_tags @> CAST(:moodTag").append(i).append(" AS jsonb)");
        }
        return filter.append(")\n").toString();
    }

    /**
     * 정렬 방향이 섞여 있어(rank_score만 DESC) 부호를 뒤집어 전부 오름차순으로 만든 뒤 행 비교로 넘긴다.
     */
    private String personalizedCursorPredicate(FeedCursor cursor, Map<String, Object> params) {
        if (cursor == null) {
            return "";
        }
        params.put("cursorSeen", cursor.seen());
        params.put("cursorNegRank", -cursor.rankScore());
        params.put("cursorShuffleKey", cursor.shuffleKey());
        params.put("cursorSpotId", cursor.spotId());
        return "WHERE (seen, -rank_score, shuffle_key, spot_id)"
                + " > (:cursorSeen, :cursorNegRank, :cursorShuffleKey, :cursorSpotId)\n";
    }

    private String guestCursorPredicate(FeedCursor cursor, Map<String, Object> params) {
        if (cursor == null) {
            return "";
        }
        params.put("cursorNegRank", -cursor.rankScore());
        params.put("cursorShuffleKey", cursor.shuffleKey());
        params.put("cursorSpotId", cursor.spotId());
        return "WHERE (-rank_score, shuffle_key, spot_id)"
                + " > (:cursorNegRank, :cursorShuffleKey, :cursorSpotId)\n";
    }

    private List<FeedSpotRow> execute(PreparedSql prepared) {
        Query query = em.createNativeQuery(prepared.sql());
        prepared.params().forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(row -> new FeedSpotRow(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        RegionNames.toEnglishArea((String) row[3]),
                        (Boolean) row[4],
                        ((Number) row[5]).intValue(),
                        ((Number) row[6]).longValue(),
                        (String) row[7]
                ))
                .toList();
    }

    private String toJsonArray(String moodTagKey) {
        return "[\"" + moodTagKey + "\"]";
    }

    record PreparedSql(String sql, Map<String, Object> params) {
    }
}
