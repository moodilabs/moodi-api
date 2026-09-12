package com.moodi.member.infrastructure.support;

import com.moodi.member.application.InquiryCountReader;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InquiryCountReaderAdapter implements InquiryCountReader {

    private static final String SQL = "SELECT COUNT(*) FROM inquiry WHERE member_id = :memberId";

    private final EntityManager em;

    public InquiryCountReaderAdapter(EntityManager em) {
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
