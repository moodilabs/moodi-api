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
        // 예약 발행을 존중한다 — visible 만 보면 미래 날짜로 잡아 둔 공지가 등록 즉시,
        // 그것도 publishedAt DESC 정렬 탓에 목록 맨 위에 뜬다. 약관은 이미
        // effectiveAt <= today 로 같은 규칙을 지킨다. 관리자 목록은 예약분까지
        // 봐야 하므로 이 조건은 공개 조회에만 건다.
        return findAll(null, true, LocalDate.now(), cursorPublishedAt, cursorId, limit);
    }

    @Override
    public List<Notice> findAll(NoticeType type, Boolean visible, LocalDate cursorPublishedAt, Long cursorId,
                                int limit) {
        return findAll(type, visible, null, cursorPublishedAt, cursorId, limit);
    }

    private List<Notice> findAll(NoticeType type, Boolean visible, LocalDate publishedOnOrBefore,
                                 LocalDate cursorPublishedAt, Long cursorId, int limit) {
        StringBuilder jpql = new StringBuilder("SELECT n FROM Notice n WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (publishedOnOrBefore != null) {
            jpql.append(" AND n.publishedAt <= :publishedOnOrBefore");
            params.put("publishedOnOrBefore", publishedOnOrBefore);
        }
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
