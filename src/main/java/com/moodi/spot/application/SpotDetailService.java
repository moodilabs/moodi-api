package com.moodi.spot.application;

import com.moodi.spot.application.dto.SpotDescriptionContext;
import com.moodi.spot.application.dto.SpotDetailSnapshot;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SpotDetailService {

    private final SpotDetailReader spotDetailReader;
    private final SpotDescriptionGenerator descriptionGenerator;

    public SpotDetailService(SpotDetailReader spotDetailReader,
                             SpotDescriptionGenerator descriptionGenerator) {
        this.spotDetailReader = spotDetailReader;
        this.descriptionGenerator = descriptionGenerator;
    }

    public SpotDetailSnapshot getDetail(Long spotId, @Nullable UUID memberId) {
        SpotDetailSnapshot snapshot = spotDetailReader.read(spotId, memberId);

        SpotDescriptionContext context = SpotDescriptionContext.from(snapshot);
        String aiDescription = descriptionGenerator.getOrGenerate(context);

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
