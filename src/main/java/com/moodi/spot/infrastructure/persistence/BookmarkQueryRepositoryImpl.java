package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.application.BookmarkQueryRepository;
import com.moodi.spot.application.dto.BookmarkSpotRow;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
public class BookmarkQueryRepositoryImpl implements BookmarkQueryRepository {

    private final EntityManager em;

    public BookmarkQueryRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<BookmarkSpotRow> findByMemberLatest(UUID memberId, String area, List<String> moodTagKeys,
                                                     Long cursorId, LocalDateTime cursorCreatedAt,
                                                     int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT b.id, b.spot_id, s.area, s.district, s.latitude, s.longitude, b.created_at,
                       (SELECT COUNT(*) FROM bookmark bc WHERE bc.spot_id = b.spot_id) AS bookmark_count
                FROM bookmark b
                JOIN spot s ON s.id = b.spot_id
                WHERE b.member_id = :memberId
                  AND s.status = 'PUBLISHED'
                """);

        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);

        appendAreaFilter(sql, params, area);
        appendMoodTagFilter(sql, params, moodTagKeys);
        appendLatestCursor(sql, params, cursorId, cursorCreatedAt);

        sql.append(" ORDER BY b.created_at DESC, b.id DESC");
        sql.append(" LIMIT :limit");
        params.put("limit", size + 1);

        Query query = em.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(this::toBookmarkSpotRow)
                .toList();
    }

    @Override
    public List<BookmarkSpotRow> findByMemberPopular(UUID memberId, String area, List<String> moodTagKeys,
                                                      Long cursorSpotId, Long cursorBookmarkCount,
                                                      int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT b.id, b.spot_id, s.area, s.district, s.latitude, s.longitude, b.created_at,
                       bc.bookmark_count
                FROM bookmark b
                JOIN spot s ON s.id = b.spot_id
                JOIN (
                    SELECT spot_id, COUNT(*) AS bookmark_count
                    FROM bookmark
                    GROUP BY spot_id
                ) bc ON bc.spot_id = b.spot_id
                WHERE b.member_id = :memberId
                  AND s.status = 'PUBLISHED'
                """);

        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);

        appendAreaFilter(sql, params, area);
        appendMoodTagFilter(sql, params, moodTagKeys);
        appendPopularCursor(sql, params, cursorSpotId, cursorBookmarkCount);

        sql.append(" ORDER BY bc.bookmark_count DESC, b.spot_id DESC");
        sql.append(" LIMIT :limit");
        params.put("limit", size + 1);

        Query query = em.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(this::toBookmarkSpotRow)
                .toList();
    }

    @Override
    public void flush() {
        em.flush();
    }

    @Override
    public long countBySpotId(Long spotId) {
        String jpql = "SELECT COUNT(b) FROM Bookmark b WHERE b.spotId = :spotId";
        return em.createQuery(jpql, Long.class)
                .setParameter("spotId", spotId)
                .getSingleResult();
    }

    @Override
    public Map<Long, Long> countBySpotIds(List<Long> spotIds) {
        if (spotIds.isEmpty()) {
            return Map.of();
        }
        String jpql = "SELECT b.spotId, COUNT(b) FROM Bookmark b WHERE b.spotId IN :spotIds GROUP BY b.spotId";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(jpql)
                .setParameter("spotIds", spotIds)
                .getResultList();

        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public Set<Long> findBookmarkedSpotIds(UUID memberId, List<Long> spotIds) {
        if (spotIds.isEmpty()) {
            return Set.of();
        }
        String jpql = "SELECT b.spotId FROM Bookmark b WHERE b.memberId = :memberId AND b.spotId IN :spotIds";
        List<Long> result = em.createQuery(jpql, Long.class)
                .setParameter("memberId", memberId)
                .setParameter("spotIds", spotIds)
                .getResultList();
        return new HashSet<>(result);
    }

    @Override
    public int deleteByMemberIdAndSpotIds(UUID memberId, List<Long> spotIds) {
        if (spotIds.isEmpty()) {
            return 0;
        }
        String jpql = "DELETE FROM Bookmark b WHERE b.memberId = :memberId AND b.spotId IN :spotIds";
        return em.createQuery(jpql)
                .setParameter("memberId", memberId)
                .setParameter("spotIds", spotIds)
                .executeUpdate();
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

    private void appendLatestCursor(StringBuilder sql, Map<String, Object> params,
                                     Long cursorId, LocalDateTime cursorCreatedAt) {
        if (cursorId != null && cursorCreatedAt != null) {
            sql.append(" AND (b.created_at < :cursorCreatedAt");
            sql.append("      OR (b.created_at = :cursorCreatedAt AND b.id < :cursorId))");
            params.put("cursorCreatedAt", cursorCreatedAt);
            params.put("cursorId", cursorId);
        }
    }

    private void appendPopularCursor(StringBuilder sql, Map<String, Object> params,
                                      Long cursorSpotId, Long cursorBookmarkCount) {
        if (cursorSpotId != null && cursorBookmarkCount != null) {
            sql.append(" AND (bc.bookmark_count < :cursorBookmarkCount");
            sql.append("      OR (bc.bookmark_count = :cursorBookmarkCount AND b.spot_id < :cursorSpotId))");
            params.put("cursorBookmarkCount", cursorBookmarkCount);
            params.put("cursorSpotId", cursorSpotId);
        }
    }

    private BookmarkSpotRow toBookmarkSpotRow(Object[] row) {
        return new BookmarkSpotRow(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                (String) row[2],
                (String) row[3],
                row[4] != null ? ((Number) row[4]).doubleValue() : null,
                row[5] != null ? ((Number) row[5]).doubleValue() : null,
                toLocalDateTime(row[6]),
                ((Number) row[7]).longValue()
        );
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        throw new IllegalArgumentException("Cannot convert to LocalDateTime: " + value.getClass());
    }
}
