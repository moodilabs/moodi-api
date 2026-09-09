package com.moodi.route.infrastructure.persistence;

import com.moodi.route.application.RouteListRow;
import com.moodi.route.application.RouteQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    RouteListRow toRouteListRow(Object[] row) {
        return new RouteListRow(
                ((Number) row[0]).longValue(),
                (UUID) row[1],
                (String) row[2],
                toLocalDate(row[3]),
                toLocalDate(row[4]),
                ((Number) row[5]).intValue(),
                toLocalDateTime(row[6])
        );
    }

    /**
     * 네이티브 쿼리 스칼라는 Hibernate 버전·드라이버에 따라 java.time 또는 java.sql 타입으로 온다.
     * (Hibernate 7은 LocalDate/LocalDateTime을 돌려준다.) 둘 다 받아준다.
     */
    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate ld) {
            return ld;
        }
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        throw new IllegalArgumentException("Cannot convert to LocalDate: " + value.getClass());
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
