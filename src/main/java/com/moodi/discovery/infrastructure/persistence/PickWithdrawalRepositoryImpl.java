package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.PickWithdrawalRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** 자식(결과·지역) → 부모(요청) 순서로 지운다. 컨텍스트 내부 FK가 있어 순서가 중요하다. */
@Repository
public class PickWithdrawalRepositoryImpl implements PickWithdrawalRepository {

    private final EntityManager em;

    public PickWithdrawalRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<String> deleteAllByMemberId(UUID memberId) {
        @SuppressWarnings("unchecked")
        List<String> imageKeys = em.createNativeQuery(
                        "SELECT image_key FROM pick_request WHERE member_id = :memberId")
                .setParameter("memberId", memberId)
                .getResultList();
        if (imageKeys.isEmpty()) {
            return List.of();
        }
        em.createNativeQuery("DELETE FROM pick_result_spot WHERE pick_request_id IN "
                        + "(SELECT id FROM pick_request WHERE member_id = :memberId)")
                .setParameter("memberId", memberId)
                .executeUpdate();
        em.createNativeQuery("DELETE FROM pick_request_area WHERE pick_request_id IN "
                        + "(SELECT id FROM pick_request WHERE member_id = :memberId)")
                .setParameter("memberId", memberId)
                .executeUpdate();
        em.createNativeQuery("DELETE FROM pick_request WHERE member_id = :memberId")
                .setParameter("memberId", memberId)
                .executeUpdate();
        return imageKeys;
    }
}
