package com.moodi.spot.infrastructure.persistence;

import jakarta.persistence.EntityManager;

import java.util.List;

public class SpotTranslationJpaRepositoryImpl {

    private final EntityManager entityManager;

    public SpotTranslationJpaRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public List<Long> findSpotIdsWithoutTranslation(String locale) {
        return entityManager.createNativeQuery(
                        "SELECT s.id FROM spot s " +
                                "WHERE s.status = 'PUBLISHED' " +
                                "AND s.id NOT IN (" +
                                "  SELECT st.spot_id FROM spot_translation st WHERE st.locale = :locale" +
                                ")")
                .setParameter("locale", locale)
                .getResultList();
    }
}
