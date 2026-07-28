package com.moodi.spot.application;

import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SpotDescriptionWriter {

    private final SpotDescriptionRepository descriptionRepository;

    public SpotDescriptionWriter(SpotDescriptionRepository descriptionRepository) {
        this.descriptionRepository = descriptionRepository;
    }

    @Transactional
    public SpotDescription saveIfAbsent(Long spotId, String locale, String content) {
        SpotDescription description = SpotDescription.create(spotId, locale, content);
        return descriptionRepository.saveIfAbsent(description);
    }
}
