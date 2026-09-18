package com.moodi.support.infrastructure.member;

import com.moodi.support.application.PolicyAgreementReader;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 회원 컨텍스트의 `member_agreement`를 네이티브 SQL로 읽는다 (경계를 넘는 연관관계 없음). */
@Component
public class PolicyAgreementReaderAdapter implements PolicyAgreementReader {

    private static final String COUNT_SQL =
            "SELECT COUNT(*) FROM member_agreement WHERE policy_id = :policyId";
    private static final String AGREED_IDS_SQL =
            "SELECT DISTINCT policy_id FROM member_agreement WHERE policy_id IN (:ids)";

    private final EntityManager em;

    public PolicyAgreementReaderAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public boolean hasAgreement(Long policyId) {
        Number count = (Number) em.createNativeQuery(COUNT_SQL)
                .setParameter("policyId", policyId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public Set<Long> findAgreedIds(Collection<Long> policyIds) {
        if (policyIds.isEmpty()) {
            return Set.of();
        }
        @SuppressWarnings("unchecked")
        List<Number> rows = em.createNativeQuery(AGREED_IDS_SQL)
                .setParameter("ids", policyIds)
                .getResultList();
        Set<Long> result = new HashSet<>();
        for (Number row : rows) {
            result.add(row.longValue());
        }
        return result;
    }
}
