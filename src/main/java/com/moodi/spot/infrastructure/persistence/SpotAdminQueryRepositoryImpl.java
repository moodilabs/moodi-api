package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.application.SpotAdminQueryRepository;
import com.moodi.spot.application.dto.SpotAdminFilter;
import com.moodi.spot.application.dto.SpotAdminRow;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SpotAdminQueryRepositoryImpl implements SpotAdminQueryRepository {

    private static final String TITLE_LOCALE = "en-US";

    private final EntityManager em;

    public SpotAdminQueryRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<SpotAdminRow> findAll(SpotAdminFilter filter, Long cursorId, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT s.id, t.title, s.content_type, s.area, s.district, s.status, s.route_excluded,
                       (SELECT COUNT(*) FROM bookmark b WHERE b.spot_id = s.id) AS bookmark_count,
                       s.created_at, s.status_changed_at
                FROM spot s
                LEFT JOIN spot_translation t ON t.spot_id = s.id AND t.locale = :locale
                WHERE 1 = 1
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("locale", TITLE_LOCALE);

        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            sql.append(" AND (LOWER(t.title) LIKE :keyword OR s.content_id LIKE :keyword)");
            params.put("keyword", "%" + filter.keyword().trim().toLowerCase() + "%");
        }
        if (filter.status() != null) {
            sql.append(" AND s.status = :status");
            params.put("status", filter.status().name());
        }
        if (filter.area() != null && !filter.area().isBlank()) {
            sql.append(" AND s.area = :area");
            params.put("area", filter.area().trim());
        }
        if (cursorId != null) {
            sql.append(" AND s.id < :cursorId");
            params.put("cursorId", cursorId);
        }
        sql.append(" ORDER BY s.id DESC LIMIT :limit");
        params.put("limit", limit);

        Query query = em.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::toRow).toList();
    }

    private SpotAdminRow toRow(Object[] row) {
        return new SpotAdminRow(
                ((Number) row[0]).longValue(),
                (String) row[1],
                SpotContentType.valueOf((String) row[2]),
                (String) row[3],
                (String) row[4],
                SpotStatus.valueOf((String) row[5]),
                (Boolean) row[6],
                ((Number) row[7]).longValue(),
                toLocalDateTime(row[8]),
                toLocalDateTime(row[9])
        );
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }
}
