package com.moodi.member.infrastructure.spot;

import com.moodi.member.application.BookmarkCountReader;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 스팟 컨텍스트의 `bookmark` 테이블을 읽는 어댑터. 경계를 넘는 JPA 연관관계 대신 네이티브 SQL로 수만 센다.
 */
@Component
public class BookmarkCountReaderAdapter implements BookmarkCountReader {

    private static final String SQL = "SELECT COUNT(*) FROM bookmark WHERE member_id = :memberId";

    private final EntityManager em;

    public BookmarkCountReaderAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public long countByMemberId(UUID memberId) {
        Number count = (Number) em.createNativeQuery(SQL)
                .setParameter("memberId", memberId)
                .getSingleResult();
        return count.longValue();
    }
}
