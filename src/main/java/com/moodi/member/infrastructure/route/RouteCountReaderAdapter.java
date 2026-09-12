package com.moodi.member.infrastructure.route;

import com.moodi.member.application.RouteCountReader;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 루트 컨텍스트의 `route` 테이블을 읽는 어댑터. 소프트 삭제된 루트는 제외한다.
 */
@Component
public class RouteCountReaderAdapter implements RouteCountReader {

    private static final String SQL =
            "SELECT COUNT(*) FROM route WHERE member_id = :memberId AND deleted_at IS NULL";

    private final EntityManager em;

    public RouteCountReaderAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public long countActiveByMemberId(UUID memberId) {
        Number count = (Number) em.createNativeQuery(SQL)
                .setParameter("memberId", memberId)
                .getSingleResult();
        return count.longValue();
    }
}
