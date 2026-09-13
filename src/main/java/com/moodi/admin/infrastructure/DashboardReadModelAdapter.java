package com.moodi.admin.infrastructure;

import com.moodi.admin.application.DashboardReadModel;
import com.moodi.admin.application.dto.DashboardSummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 테이블별 COUNT 네이티브 SQL. 데이터가 커지면 일별 스냅샷 테이블로 바꾼다.
 * 탈퇴 회원은 status가 PENDING으로 남으므로 상태 집계는 모두 `deleted_at IS NULL`을 건다.
 */
@Component
public class DashboardReadModelAdapter implements DashboardReadModel {

    private final EntityManager em;

    public DashboardReadModelAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public DashboardSummary.Members countMembers(LocalDateTime todayStart, LocalDateTime weekStart) {
        return new DashboardSummary.Members(
                count("SELECT COUNT(*) FROM member"),
                count("SELECT COUNT(*) FROM member WHERE deleted_at IS NULL AND status = 'ACTIVE'"),
                count("SELECT COUNT(*) FROM member WHERE deleted_at IS NULL AND status = 'PENDING'"),
                count("SELECT COUNT(*) FROM member WHERE deleted_at IS NULL AND status = 'SUSPENDED'"),
                count("SELECT COUNT(*) FROM member WHERE deleted_at IS NOT NULL"),
                count("SELECT COUNT(*) FROM member WHERE created_at >= :since", Map.of("since", todayStart)),
                count("SELECT COUNT(*) FROM member WHERE created_at >= :since", Map.of("since", weekStart))
        );
    }

    @Override
    public DashboardSummary.Content countContent() {
        return new DashboardSummary.Content(
                count("SELECT COUNT(*) FROM spot WHERE status = 'PUBLISHED'"),
                count("SELECT COUNT(*) FROM bookmark"),
                count("SELECT COUNT(*) FROM route WHERE deleted_at IS NULL"),
                count("SELECT COUNT(*) FROM route WHERE deleted_at IS NULL AND is_shared = TRUE"),
                count("SELECT COUNT(*) FROM pick_request")
        );
    }

    @Override
    public DashboardSummary.Inquiries countInquiries(LocalDateTime weekStart) {
        return new DashboardSummary.Inquiries(
                count("SELECT COUNT(*) FROM inquiry WHERE status = 'RECEIVED'"),
                count("SELECT COUNT(*) FROM inquiry WHERE answered_at >= :since", Map.of("since", weekStart))
        );
    }

    private long count(String sql) {
        return count(sql, Map.of());
    }

    private long count(String sql, Map<String, Object> params) {
        Query query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return ((Number) query.getSingleResult()).longValue();
    }
}
