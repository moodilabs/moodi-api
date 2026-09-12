package com.moodi.support.infrastructure.member;

import com.moodi.support.application.MemberSummaryReader;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 회원 컨텍스트의 `member`를 네이티브 SQL로 읽는다 (경계를 넘는 연관관계 없음). */
@Component
public class MemberSummaryReaderAdapter implements MemberSummaryReader {

    private static final String SQL =
            "SELECT id, nickname, email, deleted_at FROM member WHERE id IN (:ids)";

    private final EntityManager em;

    public MemberSummaryReaderAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Map<UUID, MemberSummary> readByIds(Collection<UUID> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(SQL)
                .setParameter("ids", memberIds)
                .getResultList();

        Map<UUID, MemberSummary> result = new HashMap<>();
        for (Object[] row : rows) {
            UUID id = toUuid(row[0]);
            result.put(id, new MemberSummary(id, (String) row[1], (String) row[2], row[3] != null));
        }
        return result;
    }

    private UUID toUuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }
}
