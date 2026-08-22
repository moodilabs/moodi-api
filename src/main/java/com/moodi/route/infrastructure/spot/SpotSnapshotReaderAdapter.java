package com.moodi.route.infrastructure.spot;

import com.moodi.route.application.SpotSnapshot;
import com.moodi.route.application.SpotSnapshotReader;
import com.moodi.route.domain.RouteSpotType;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
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

    private static final String TRANSLATION_LOCALE = "ko-KR";
    private static final String DESCRIPTION_LOCALE = "en-US";

    private final SpotRepository spotRepository;
    private final SpotTranslationRepository translationRepository;
    private final SpotImageRepository imageRepository;
    private final SpotDescriptionRepository descriptionRepository;

    public SpotSnapshotReaderAdapter(SpotRepository spotRepository,
                                     SpotTranslationRepository translationRepository,
                                     SpotImageRepository imageRepository,
                                     SpotDescriptionRepository descriptionRepository) {
        this.spotRepository = spotRepository;
        this.translationRepository = translationRepository;
        this.imageRepository = imageRepository;
        this.descriptionRepository = descriptionRepository;
    }

    @Override
    public List<SpotSnapshot> readBySpotIds(List<Long> spotIds) {
        List<Spot> spots = spotRepository.findByIdIn(spotIds).stream()
                .filter(spot -> spot.getStatus() == SpotStatus.PUBLISHED)
                .filter(spot -> !spot.isRouteExcluded())
                .toList();

        List<Long> filteredIds = spots.stream().map(Spot::getId).toList();

        Map<Long, SpotTranslation> translationMap = translationRepository.findBySpotIdIn(filteredIds).stream()
                .filter(t -> TRANSLATION_LOCALE.equals(t.getLocale()))
                .collect(Collectors.toMap(SpotTranslation::getSpotId, Function.identity(), (a, b) -> a));

        Map<Long, SpotImage> primaryImageMap = imageRepository.findBySpotIdInAndIsPrimaryTrue(filteredIds).stream()
                .collect(Collectors.toMap(SpotImage::getSpotId, Function.identity(), (a, b) -> a));

        Map<Long, String> descriptionMap = descriptionRepository
                .findBySpotIdInAndLocale(filteredIds, DESCRIPTION_LOCALE).stream()
                .collect(Collectors.toMap(SpotDescription::getSpotId, SpotDescription::getContent, (a, b) -> a));

        return spots.stream()
                .map(spot -> {
                    SpotTranslation translation = translationMap.get(spot.getId());
                    SpotImage primaryImage = primaryImageMap.get(spot.getId());
                    String description = descriptionMap.get(spot.getId());
                    return new SpotSnapshot(
                            spot.getId(),
                            translation != null ? translation.getTitle() : null,
                            primaryImage != null ? primaryImage.getImageUrl() : null,
                            spot.getArea(),
                            spot.getDistrict(),
                            spot.getLatitude(),
                            spot.getLongitude(),
                            RouteSpotType.valueOf(spot.getContentType().name()),
                            description
                    );
                })
                .toList();
    }
}
