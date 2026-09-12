package com.moodi.member.infrastructure.persistence;

import com.moodi.member.application.MemberAdminQueryRepository;
import com.moodi.member.application.dto.MemberAdminFilter;
import com.moodi.member.application.dto.MemberAdminRow;
import com.moodi.member.application.dto.MemberAdminStatus;
import com.moodi.member.application.dto.MemberDailyStat;
import com.moodi.member.domain.MemberStatus;
import com.moodi.member.domain.OAuthProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Repository
public class MemberAdminQueryRepositoryImpl implements MemberAdminQueryRepository {

    private final EntityManager em;

    public MemberAdminQueryRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<MemberAdminRow> findAll(MemberAdminFilter filter, LocalDateTime cursorCreatedAt, UUID cursorId,
                                        int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, provider, email, nickname, country, status, created_at, deleted_at, suspended_at
                FROM member
                WHERE 1 = 1
                """);
        Map<String, Object> params = new HashMap<>();

        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            sql.append(" AND (LOWER(nickname) LIKE :keyword OR LOWER(email) LIKE :keyword)");
            params.put("keyword", "%" + filter.keyword().trim().toLowerCase() + "%");
        }
        if (filter.provider() != null) {
            sql.append(" AND provider = :provider");
            params.put("provider", filter.provider().name());
        }
        appendStatusFilter(sql, params, filter.status());
        if (cursorCreatedAt != null && cursorId != null) {
            sql.append(" AND (created_at < :cursorCreatedAt OR (created_at = :cursorCreatedAt AND id < :cursorId))");
            params.put("cursorCreatedAt", cursorCreatedAt);
            params.put("cursorId", cursorId);
        }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT :limit");
        params.put("limit", limit);

        Query query = em.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(this::toRow).toList();
    }

    /** 탈퇴는 `deleted_at`으로, 나머지는 `status`로 거른다. 탈퇴 회원은 status가 PENDING으로 남아 있어 반드시 제외한다. */
    private void appendStatusFilter(StringBuilder sql, Map<String, Object> params, MemberAdminStatus status) {
        if (status == null) {
            return;
        }
        if (status == MemberAdminStatus.WITHDRAWN) {
            sql.append(" AND deleted_at IS NOT NULL");
            return;
        }
        sql.append(" AND deleted_at IS NULL AND status = :status");
        params.put("status", status.name());
    }

    @Override
    public List<MemberDailyStat> countDaily(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        Map<LocalDate, long[]> byDate = new TreeMap<>();
        countInto(byDate, "created_at", start, end, 0);
        countInto(byDate, "deleted_at", start, end, 1);

        return byDate.entrySet().stream()
                .map(entry -> new MemberDailyStat(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private void countInto(Map<LocalDate, long[]> byDate, String column, LocalDateTime start, LocalDateTime end,
                           int slot) {
        String sql = "SELECT CAST(" + column + " AS DATE) AS d, COUNT(*) FROM member"
                + " WHERE " + column + " >= :start AND " + column + " < :end GROUP BY CAST(" + column + " AS DATE)";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            byDate.computeIfAbsent(date, ignored -> new long[2])[slot] = ((Number) row[1]).longValue();
        }
    }

    private MemberAdminRow toRow(Object[] row) {
        LocalDateTime deletedAt = toLocalDateTime(row[7]);
        return new MemberAdminRow(
                toUuid(row[0]),
                OAuthProvider.valueOf((String) row[1]),
                (String) row[2],
                (String) row[3],
                (String) row[4],
                MemberAdminStatus.of(MemberStatus.valueOf((String) row[5]), deletedAt != null),
                toLocalDateTime(row[6]),
                deletedAt,
                toLocalDateTime(row[8])
        );
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        // H2는 네이티브 쿼리에서 UUID를 byte[]로 돌려준다. 운영(PostgreSQL)은 UUID 객체.
        if (value instanceof byte[] bytes) {
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        return UUID.fromString(value.toString());
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

    private LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        return (LocalDate) value;
    }
}
