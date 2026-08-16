package com.moodi.spot.application;

import com.moodi.spot.application.dto.SpotDetailSnapshot;
import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SpotDetailService {

    private static final String DESCRIPTION_LOCALE = "en-US";

    private final SpotDetailReader spotDetailReader;
    private final SpotDescriptionRepository descriptionRepository;

    public SpotDetailService(SpotDetailReader spotDetailReader,
                             SpotDescriptionRepository descriptionRepository) {
        this.spotDetailReader = spotDetailReader;
        this.descriptionRepository = descriptionRepository;
    }

    public SpotDetailSnapshot getDetail(Long spotId, @Nullable UUID memberId) {
        SpotDetailSnapshot snapshot = spotDetailReader.read(spotId, memberId);

        String aiDescription = descriptionRepository
                .findBySpotIdAndLocale(spotId, DESCRIPTION_LOCALE)
                .map(SpotDescription::getContent)
                .orElse(null);

        return new SpotDetailSnapshot(
                snapshot.spotId(),
                snapshot.title(),
                snapshot.area(),
                snapshot.district(),
                snapshot.overview(),
                snapshot.homepage(),
                snapshot.tel(),
                snapshot.contentType(),
                snapshot.moodTags(),
                snapshot.moodTagKeys(),
                snapshot.images(),
                snapshot.bookmarkCount(),
                snapshot.bookmarked(),
                snapshot.latitude(),
                snapshot.longitude(),
                snapshot.addr1(),
                snapshot.addr2(),
                snapshot.similarMoodSpots(),
                snapshot.popularAreaSpots(),
                aiDescription
        );
    }
}
