package com.moodi.support.infrastructure.persistence;

import com.moodi.support.application.NoticeQueryRepository;
import com.moodi.support.domain.Notice;
import com.moodi.support.domain.NoticeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class NoticeQueryRepositoryImpl implements NoticeQueryRepository {

    private final EntityManager em;

    public NoticeQueryRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Notice> findVisible(LocalDate cursorPublishedAt, Long cursorId, int limit) {
        return findAll(null, true, cursorPublishedAt, cursorId, limit);
    }

    @Override
    public List<Notice> findAll(NoticeType type, Boolean visible, LocalDate cursorPublishedAt, Long cursorId,
                                int limit) {
        StringBuilder jpql = new StringBuilder("SELECT n FROM Notice n WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (type != null) {
            jpql.append(" AND n.type = :type");
            params.put("type", type);
        }
        if (visible != null) {
            jpql.append(" AND n.visible = :visible");
            params.put("visible", visible);
        }
        if (cursorPublishedAt != null && cursorId != null) {
            jpql.append(" AND (n.publishedAt < :cursorPublishedAt")
                    .append(" OR (n.publishedAt = :cursorPublishedAt AND n.id < :cursorId))");
            params.put("cursorPublishedAt", cursorPublishedAt);
            params.put("cursorId", cursorId);
        }
        jpql.append(" ORDER BY n.publishedAt DESC, n.id DESC");

        TypedQuery<Notice> query = em.createQuery(jpql.toString(), Notice.class);
        params.forEach(query::setParameter);
        return query.setMaxResults(limit).getResultList();
    }
}
