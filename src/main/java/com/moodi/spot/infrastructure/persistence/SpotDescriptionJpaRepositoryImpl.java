package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.domain.SpotDescription;
import jakarta.persistence.EntityManager;

import java.util.List;

public class SpotDescriptionJpaRepositoryImpl {

    private final EntityManager entityManager;

    public SpotDescriptionJpaRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public SpotDescription saveIfAbsent(SpotDescription description) {
        entityManager.createNativeQuery(
                        "INSERT INTO spot_description (spot_id, locale, content, created_at, updated_at) " +
                                "VALUES (:spotId, :locale, :content, now(), now()) " +
                                "ON CONFLICT (spot_id, locale) DO NOTHING")
                .setParameter("spotId", description.getSpotId())
                .setParameter("locale", description.getLocale())
                .setParameter("content", description.getContent())
                .executeUpdate();

        return entityManager.createQuery(
                        "SELECT d FROM SpotDescription d WHERE d.spotId = :spotId AND d.locale = :locale",
                        SpotDescription.class)
                .setParameter("spotId", description.getSpotId())
                .setParameter("locale", description.getLocale())
                .getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Long> findSpotIdsWithoutDescription(String locale) {
        return entityManager.createNativeQuery(
                        "SELECT s.id FROM spot s " +
                                "WHERE s.status = 'PUBLISHED' " +
                                "AND s.route_excluded = false " +
                                "AND s.id NOT IN (" +
                                "  SELECT sd.spot_id FROM spot_description sd WHERE sd.locale = :locale" +
                                ")")
                .setParameter("locale", locale)
                .getResultList();
    }
}
