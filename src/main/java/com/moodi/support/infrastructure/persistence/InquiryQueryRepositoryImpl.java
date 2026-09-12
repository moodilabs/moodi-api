package com.moodi.support.infrastructure.persistence;

import com.moodi.support.application.InquiryQueryRepository;
import com.moodi.support.domain.Inquiry;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class InquiryQueryRepositoryImpl implements InquiryQueryRepository {

    private final EntityManager em;

    public InquiryQueryRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Inquiry> findByMember(UUID memberId, LocalDateTime cursorCreatedAt, UUID cursorId, int limit) {
        return query("i.memberId = :memberId", Map.of("memberId", memberId), cursorCreatedAt, cursorId, limit);
    }

    @Override
    public List<Inquiry> findAll(InquiryStatus status, InquiryTopic topic, LocalDateTime cursorCreatedAt,
                                 UUID cursorId, int limit) {
        StringBuilder where = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (status != null) {
            where.append(" AND i.status = :status");
            params.put("status", status);
        }
        if (topic != null) {
            where.append(" AND i.topic = :topic");
            params.put("topic", topic);
        }
        return query(where.toString(), params, cursorCreatedAt, cursorId, limit);
    }

    /**
     * UUID id는 순서가 없어 같은 createdAt 안에서의 타이브레이커로만 쓴다 — 페이지 경계에서 중복·누락만 막으면 된다.
     */
    private List<Inquiry> query(String where, Map<String, Object> baseParams, LocalDateTime cursorCreatedAt,
                                UUID cursorId, int limit) {
        StringBuilder jpql = new StringBuilder("SELECT i FROM Inquiry i WHERE ").append(where);
        Map<String, Object> params = new HashMap<>(baseParams);
        if (cursorCreatedAt != null && cursorId != null) {
            jpql.append(" AND (i.createdAt < :cursorCreatedAt")
                    .append(" OR (i.createdAt = :cursorCreatedAt AND i.id < :cursorId))");
            params.put("cursorCreatedAt", cursorCreatedAt);
            params.put("cursorId", cursorId);
        }
        jpql.append(" ORDER BY i.createdAt DESC, i.id DESC");

        TypedQuery<Inquiry> query = em.createQuery(jpql.toString(), Inquiry.class);
        params.forEach(query::setParameter);
        return query.setMaxResults(limit).getResultList();
    }
}
