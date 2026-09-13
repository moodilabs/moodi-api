package com.moodi.admin.infrastructure;

import com.moodi.admin.application.ApiRequestLogQueryRepository;
import com.moodi.admin.application.dto.ApiRequestLogFilter;
import com.moodi.admin.application.dto.ApiRequestLogItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 회원 닉네임·이메일은 member 테이블을 네이티브 SQL로 LEFT JOIN — 경계 너머 JPA 연관은 걸지 않는다. */
@Repository
public class ApiRequestLogQueryRepositoryImpl implements ApiRequestLogQueryRepository {

    private final EntityManager em;

    public ApiRequestLogQueryRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<ApiRequestLogItem> findAll(ApiRequestLogFilter filter, Long cursorId, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT l.id, l.member_id, m.nickname, m.email, l.method, l.path, l.status_code, l.duration_ms,
                       l.request_id, l.created_at
                FROM api_request_log l
                LEFT JOIN member m ON m.id = l.member_id
                WHERE 1 = 1
                """);
        Map<String, Object> params = new HashMap<>();
        if (filter.memberId() != null) {
            sql.append(" AND l.member_id = :memberId");
            params.put("memberId", filter.memberId());
        }
        if (filter.method() != null && !filter.method().isBlank()) {
            sql.append(" AND l.method = :method");
            params.put("method", filter.method().trim().toUpperCase());
        }
        if (filter.path() != null && !filter.path().isBlank()) {
            sql.append(" AND l.path LIKE :path");
            params.put("path", "%" + filter.path().trim() + "%");
        }
        if (filter.statusClass() != null) {
            sql.append(" AND l.status_code >= :statusFrom AND l.status_code < :statusTo");
            params.put("statusFrom", filter.statusClass() * 100);
            params.put("statusTo", filter.statusClass() * 100 + 100);
        }
        if (cursorId != null) {
            sql.append(" AND l.id < :cursorId");
            params.put("cursorId", cursorId);
        }
        sql.append(" ORDER BY l.id DESC LIMIT :limit");
        params.put("limit", limit);

        Query query = em.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::toItem).toList();
    }

    private ApiRequestLogItem toItem(Object[] row) {
        return new ApiRequestLogItem(
                ((Number) row[0]).longValue(),
                toUuid(row[1]),
                (String) row[2],
                (String) row[3],
                (String) row[4],
                (String) row[5],
                ((Number) row[6]).intValue(),
                ((Number) row[7]).intValue(),
                (String) row[8],
                toLocalDateTime(row[9])
        );
    }

    /** PostgreSQL은 UUID, H2(테스트)는 byte[16]로 돌려준다. */
    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }
}
