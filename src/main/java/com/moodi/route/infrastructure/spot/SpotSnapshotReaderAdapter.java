package com.moodi.route.infrastructure.spot;

import com.moodi.route.application.SpotSnapshot;
import com.moodi.route.application.SpotSnapshotReader;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotStatus;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotTranslationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class SpotSnapshotReaderAdapter implements SpotSnapshotReader {

    private static final String DEFAULT_LOCALE = "ko-KR";

    private final SpotRepository spotRepository;
    private final SpotTranslationRepository translationRepository;
    private final SpotImageRepository imageRepository;

    public SpotSnapshotReaderAdapter(SpotRepository spotRepository,
                                     SpotTranslationRepository translationRepository,
                                     SpotImageRepository imageRepository) {
        this.spotRepository = spotRepository;
        this.translationRepository = translationRepository;
        this.imageRepository = imageRepository;
    }

    @Override
    public List<SpotSnapshot> readBySpotIds(List<Long> spotIds) {
        List<Spot> spots = spotRepository.findByIdIn(spotIds).stream()
                .filter(spot -> spot.getStatus() == SpotStatus.PUBLISHED)
                .filter(spot -> !spot.isRouteExcluded())
                .toList();

        Map<Long, SpotTranslation> translationMap = translationRepository.findBySpotIdIn(
                spots.stream().map(Spot::getId).toList()
        ).stream()
                .filter(t -> DEFAULT_LOCALE.equals(t.getLocale()))
                .collect(Collectors.toMap(SpotTranslation::getSpotId, Function.identity()));

        Map<Long, SpotImage> primaryImageMap = imageRepository.findBySpotIdInAndPrimaryTrue(
                spots.stream().map(Spot::getId).toList()
        ).stream()
                .collect(Collectors.toMap(SpotImage::getSpotId, Function.identity(), (a, b) -> a));

        return spots.stream()
                .map(spot -> {
                    SpotTranslation translation = translationMap.get(spot.getId());
                    SpotImage primaryImage = primaryImageMap.get(spot.getId());
                    return new SpotSnapshot(
                            spot.getId(),
                            translation != null ? translation.getTitle() : null,
                            primaryImage != null ? primaryImage.getImageUrl() : null,
                            spot.getArea(),
                            spot.getDistrict(),
                            spot.getLatitude(),
                            spot.getLongitude(),
                            spot.getContentType(),
                            translation != null ? translation.getOverview() : null
                    );
                })
                .toList();
    }
}
