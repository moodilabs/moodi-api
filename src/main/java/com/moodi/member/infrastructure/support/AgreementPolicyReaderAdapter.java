package com.moodi.member.infrastructure.support;

import com.moodi.member.application.AgreementPolicyReader;
import com.moodi.member.domain.AgreementType;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Repository
public class AgreementPolicyReaderAdapter implements AgreementPolicyReader {

    private final EntityManager em;
    private final Clock clock;

    public AgreementPolicyReaderAdapter(EntityManager em, Clock clock) {
        this.em = em;
        this.clock = clock;
    }
    @Override
    public Version findCurrent(AgreementType type, String locale) {
        if (type == AgreementType.AGE_OVER_14) return null;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("SELECT id, version, locale FROM policy WHERE type = :type AND locale = :locale AND enabled = true AND visible = true AND effective_at <= :today ORDER BY effective_at DESC, id DESC")
                .setParameter("type", type.name()).setParameter("locale", locale).setParameter("today", LocalDate.now(clock)).setMaxResults(1).getResultList();
        return rows.isEmpty() ? null : new Version(((Number) rows.getFirst()[0]).longValue(), (String) rows.getFirst()[1], (String) rows.getFirst()[2]);
    }
}
