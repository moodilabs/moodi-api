package com.moodi.route.infrastructure.persistence;

import com.moodi.route.application.RouteListRow;
import com.moodi.route.application.RouteQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RouteQueryRepositoryImpl implements RouteQueryRepository {

    private final EntityManager em;

    @Override
    public List<RouteListRow> findByMemberLatest(UUID memberId, Long cursorId,
                                                  LocalDateTime cursorUpdatedAt, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.id, r.public_id, r.title, r.start_date, r.end_date,
                       (SELECT COUNT(*) FROM route_spot rs
                            JOIN route_day rd ON rd.id = rs.route_day_id
                        WHERE rd.route_id = r.id) AS spot_count,
                       r.updated_at
                FROM route r
                WHERE r.member_id = :memberId
                  AND r.deleted_at IS NULL
                """);

        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);

        appendCursor(sql, params, cursorId, cursorUpdatedAt);

        sql.append(" ORDER BY r.updated_at DESC, r.id DESC");
        sql.append(" LIMIT :limit");
        params.put("limit", size + 1);

        Query query = em.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        return rows.stream()
                .map(this::toRouteListRow)
                .toList();
    }

    private void appendCursor(StringBuilder sql, Map<String, Object> params,
                               Long cursorId, LocalDateTime cursorUpdatedAt) {
        if (cursorId == null || cursorUpdatedAt == null) {
            return;
        }
        sql.append("""
                  AND (r.updated_at < :cursorUpdatedAt
                       OR (r.updated_at = :cursorUpdatedAt AND r.id < :cursorId))
                """);
        params.put("cursorUpdatedAt", cursorUpdatedAt);
        params.put("cursorId", cursorId);
    }

    private RouteListRow toRouteListRow(Object[] row) {
        return new RouteListRow(
                ((Number) row[0]).longValue(),
                (UUID) row[1],
                (String) row[2],
                ((java.sql.Date) row[3]).toLocalDate(),
                ((java.sql.Date) row[4]).toLocalDate(),
                ((Number) row[5]).intValue(),
                ((Timestamp) row[6]).toLocalDateTime()
        );
    }
}
