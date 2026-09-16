package com.moodi.route.infrastructure.spot;

import com.moodi.route.application.SpotSnapshot;
import com.moodi.route.application.SpotSnapshotReader;
import com.moodi.route.domain.RouteSpotType;
import com.moodi.spot.application.RegionDictionary;
import com.moodi.shared.mood.MoodTag;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.domain.SpotMoodRepository;
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

    private static final String TRANSLATION_LOCALE = "en-US";
    private static final String DESCRIPTION_LOCALE = "en-US";

    private final SpotRepository spotRepository;
    private final SpotTranslationRepository translationRepository;
    private final SpotImageRepository imageRepository;
    private final SpotDescriptionRepository descriptionRepository;
    private final SpotMoodRepository spotMoodRepository;

    public SpotSnapshotReaderAdapter(SpotRepository spotRepository,
                                     SpotTranslationRepository translationRepository,
                                     SpotImageRepository imageRepository,
                                     SpotDescriptionRepository descriptionRepository,
                                     SpotMoodRepository spotMoodRepository) {
        this.spotRepository = spotRepository;
        this.translationRepository = translationRepository;
        this.imageRepository = imageRepository;
        this.descriptionRepository = descriptionRepository;
        this.spotMoodRepository = spotMoodRepository;
    }

    @Override
    public List<SpotSnapshot> readBySpotIds(List<Long> spotIds) {
        // 사용자가 ID로 직접 지정한 스팟(루트 생성 기준 스팟·저장/수정 대상)을 그대로 조회한다.
        // routeExcluded·비관광 쇼핑 제외는 "추천" 정책이라 여기 적용하면 안 된다 — 그 정책은
        // SpotRecommendationReaderAdapter(루트 자동 채움)와 FeedSpotReaderAdapter(피드)에만 둔다.
        List<Spot> spots = spotRepository.findByIdIn(spotIds).stream()
                .filter(spot -> spot.getStatus() == SpotStatus.PUBLISHED)
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

        Map<Long, List<String>> moodTagMap = spotMoodRepository.findBySpotIdIn(filteredIds).stream()
                .collect(Collectors.toMap(
                        SpotMood::getSpotId,
                        sm -> sm.getMoodTags().stream().map(MoodTag::getKey).toList(),
                        (a, b) -> a));

        return spots.stream()
                .map(spot -> {
                    SpotTranslation translation = translationMap.get(spot.getId());
                    SpotImage primaryImage = primaryImageMap.get(spot.getId());
                    String description = descriptionMap.get(spot.getId());
                    List<String> moodTags = moodTagMap.getOrDefault(spot.getId(), List.of());
                    return new SpotSnapshot(
                            spot.getId(),
                            translation != null ? translation.getTitle() : null,
                            primaryImage != null ? primaryImage.getImageUrl() : null,
                            RegionDictionary.translateArea(spot.getArea()),
                            RegionDictionary.translateDistrict(spot.getDistrict()),
                            spot.getLatitude(),
                            spot.getLongitude(),
                            RouteSpotType.valueOf(spot.getContentType().name()),
                            description,
                            moodTags
                    );
                })
                .toList();
    }
}
