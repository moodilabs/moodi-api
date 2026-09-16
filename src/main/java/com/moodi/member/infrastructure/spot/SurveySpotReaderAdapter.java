package com.moodi.member.infrastructure.spot;

import com.moodi.member.application.SurveySpotReader;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class SurveySpotReaderAdapter implements SurveySpotReader {

    private final EntityManager em;

    public SurveySpotReaderAdapter(EntityManager em) {
        this.em = em;
    }

    public boolean isAvailable(Long spotId, String imageUrl) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM spot s JOIN spot_image i ON i.spot_id = s.id WHERE s.id = :id AND s.status = 'PUBLISHED' AND i.image_url = :url")
                .setParameter("id", spotId).setParameter("url", imageUrl).getSingleResult()).longValue() > 0;
    }
}
