package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.application.SpotSearchQueryRepository;
import com.moodi.spot.application.dto.SpotSearchRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class SpotSearchQueryRepositoryImpl implements SpotSearchQueryRepository {

    private final EntityManager em;

    public SpotSearchQueryRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<SpotSearchRow> searchByBestMatch(String keyword, String area, List<String> moodTagKeys,
                                                  boolean saved, UUID memberId,
                                                  Integer cursorMatchRank, Long cursorBookmarkCount,
                                                  Long cursorSpotId, int size) {
        StringBuilder innerSql = new StringBuilder("""
                SELECT s.id, s.area, s.district,
                       (SELECT COUNT(*) FROM bookmark bc WHERE bc.spot_id = s.id) AS bookmark_count,
                       (CASE
                            WHEN st.title ILIKE :exactKeyword THEN 1
                            WHEN st.title ILIKE :startKeyword THEN 2
                            WHEN st.title ILIKE :containKeyword THEN 3
                            WHEN s.area ILIKE :containKeyword THEN 4
                            WHEN s.district ILIKE :containKeyword THEN 5
                            ELSE 6
                        END) AS match_rank
                FROM spot s
                JOIN spot_translation st ON st.spot_id = s.id AND st.locale = 'ko-KR'
                WHERE s.status = 'PUBLISHED'
                  AND (st.title ILIKE :containKeyword OR s.area ILIKE :containKeyword OR s.district ILIKE :containKeyword)
                """);

        Map<String, Object> params = new HashMap<>();
        params.put("exactKeyword", keyword);
        params.put("startKeyword", keyword + "%");
        params.put("containKeyword", "%" + keyword + "%");

        appendAreaFilter(innerSql, params, area);
        appendMoodTagFilter(innerSql, params, moodTagKeys);
        appendSavedFilter(innerSql, params, saved, memberId);

        StringBuilder sql = new StringBuilder("SELECT t.id, t.area, t.district, t.bookmark_count, t.match_rank FROM (");
        sql.append(innerSql);
        sql.append(") t");

        appendBestMatchCursor(sql, params, cursorMatchRank, cursorBookmarkCount, cursorSpotId);

        sql.append(" ORDER BY t.match_rank ASC, t.bookmark_count DESC, t.id DESC");
        sql.append(" LIMIT :limit");
        params.put("limit", size + 1);

        return executeSpotSearchQuery(sql.toString(), params, true);
    }

    @Override
    public List<SpotSearchRow> searchByMostSaved(String area, List<String> moodTagKeys,
                                                  boolean saved, UUID memberId,
                                                  Long cursorSpotId, Long cursorBookmarkCount, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT s.id, s.area, s.district,
                       (SELECT COUNT(*) FROM bookmark bc WHERE bc.spot_id = s.id) AS bookmark_count
                FROM spot s
                WHERE s.status = 'PUBLISHED'
                """);

        Map<String, Object> params = new HashMap<>();

        appendAreaFilter(sql, params, area);
        appendMoodTagFilter(sql, params, moodTagKeys);
        appendSavedFilter(sql, params, saved, memberId);
        appendMostSavedCursor(sql, params, cursorSpotId, cursorBookmarkCount);

        sql.append(" ORDER BY bookmark_count DESC, s.id DESC");
        sql.append(" LIMIT :limit");
        params.put("limit", size + 1);

        return executeSpotSearchQuery(sql.toString(), params, false);
    }

    @Override
    public Set<Long> findSpotIdsInRoute(UUID routePublicId, List<Long> spotIds) {
        if (spotIds.isEmpty()) {
            return Set.of();
        }
        String sql = """
                SELECT DISTINCT rs.spot_id
                FROM route_spot rs
                JOIN route_day rd ON rd.id = rs.route_day_id
                JOIN route r ON r.id = rd.route_id
                WHERE r.public_id = :routePublicId
                  AND r.deleted_at IS NULL
                  AND rs.spot_id IN :spotIds
                """;
        Query query = em.createNativeQuery(sql);
        query.setParameter("routePublicId", routePublicId);
        query.setParameter("spotIds", spotIds);

        @SuppressWarnings("unchecked")
        List<Number> result = query.getResultList();
        Set<Long> ids = new HashSet<>();
        for (Number id : result) {
            ids.add(id.longValue());
        }
        return ids;
    }

    private List<SpotSearchRow> executeSpotSearchQuery(String sql, Map<String, Object> params,
                                                         boolean hasMatchRank) {
        Query query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(row -> new SpotSearchRow(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        hasMatchRank ? ((Number) row[4]).intValue() : 0
                ))
                .toList();
    }

    private void appendAreaFilter(StringBuilder sql, Map<String, Object> params, String area) {
        if (area != null && !area.isBlank()) {
            sql.append(" AND s.area = :area");
            params.put("area", area);
        }
    }

    private void appendMoodTagFilter(StringBuilder sql, Map<String, Object> params, List<String> moodTagKeys) {
        if (moodTagKeys != null && !moodTagKeys.isEmpty()) {
            sql.append(" AND EXISTS (");
            sql.append("   SELECT 1 FROM spot_mood sm WHERE sm.spot_id = s.id");
            sql.append("   AND (");
            for (int i = 0; i < moodTagKeys.size(); i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                String paramName = "moodTag" + i;
                sql.append("sm.mood_tags @> CAST(:").append(paramName).append(" AS jsonb)");
                params.put(paramName, "[\"" + moodTagKeys.get(i) + "\"]");
            }
            sql.append("   )");
            sql.append(" )");
        }
    }

    private void appendSavedFilter(StringBuilder sql, Map<String, Object> params, boolean saved, UUID memberId) {
        if (saved && memberId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM bookmark b WHERE b.spot_id = s.id AND b.member_id = :memberId)");
            params.put("memberId", memberId);
        }
    }

    private void appendBestMatchCursor(StringBuilder sql, Map<String, Object> params,
                                        Integer cursorMatchRank, Long cursorBookmarkCount,
                                        Long cursorSpotId) {
        if (cursorMatchRank != null && cursorBookmarkCount != null && cursorSpotId != null) {
            sql.append("""
                     WHERE (t.match_rank > :cursorMatchRank
                            OR (t.match_rank = :cursorMatchRank AND t.bookmark_count < :cursorBookmarkCount)
                            OR (t.match_rank = :cursorMatchRank AND t.bookmark_count = :cursorBookmarkCount AND t.id < :cursorSpotId))
                    """);
            params.put("cursorMatchRank", cursorMatchRank);
            params.put("cursorBookmarkCount", cursorBookmarkCount);
            params.put("cursorSpotId", cursorSpotId);
        }
    }

    private void appendMostSavedCursor(StringBuilder sql, Map<String, Object> params,
                                        Long cursorSpotId, Long cursorBookmarkCount) {
        if (cursorSpotId != null && cursorBookmarkCount != null) {
            sql.append(" AND ((SELECT COUNT(*) FROM bookmark bc WHERE bc.spot_id = s.id) < :cursorBookmarkCount");
            sql.append("      OR ((SELECT COUNT(*) FROM bookmark bc WHERE bc.spot_id = s.id) = :cursorBookmarkCount AND s.id < :cursorSpotId))");
            params.put("cursorBookmarkCount", cursorBookmarkCount);
            params.put("cursorSpotId", cursorSpotId);
        }
    }
}
