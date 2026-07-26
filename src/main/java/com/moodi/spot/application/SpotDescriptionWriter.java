package com.moodi.spot.application;

import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class SpotDescriptionWriter {

    private final EntityManager entityManager;
    private final SpotDescriptionRepository descriptionRepository;

    public SpotDescriptionWriter(EntityManager entityManager,
                                 SpotDescriptionRepository descriptionRepository) {
        this.entityManager = entityManager;
        this.descriptionRepository = descriptionRepository;
    }

    @Transactional
    public SpotDescription saveIfAbsent(Long spotId, String locale, String content) {
        entityManager.createNativeQuery(
                        "INSERT INTO spot_description (spot_id, locale, content, created_at, updated_at) " +
                                "VALUES (:spotId, :locale, :content, now(), now()) " +
                                "ON CONFLICT (spot_id, locale) DO NOTHING")
                .setParameter("spotId", spotId)
                .setParameter("locale", locale)
                .setParameter("content", content)
                .executeUpdate();

        return descriptionRepository.findBySpotIdAndLocale(spotId, locale)
                .orElseThrow();
    }
}
