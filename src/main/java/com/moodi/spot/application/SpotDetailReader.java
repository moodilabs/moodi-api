package com.moodi.spot.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.mood.MoodTag;
import com.moodi.spot.application.dto.PopularAreaSpotItem;
import com.moodi.spot.application.dto.SimilarMoodSpotItem;
import com.moodi.spot.application.dto.SpotDetailSnapshot;
import com.moodi.spot.application.dto.SpotImageItem;
import com.moodi.spot.application.RegionDictionary;
import com.moodi.spot.domain.BookmarkRepository;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotStatus;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class SpotDetailReader {

    private static final String DEFAULT_LOCALE = "en-US";
    private static final String KOREAN_LOCALE = "ko-KR";
    private static final String KAKAO_MAP_URL_TEMPLATE = "https://map.kakao.com/link/map/%s,%s,%s";
    private static final int SIMILAR_MOOD_LIMIT = 5;
    private static final int POPULAR_AREA_LIMIT = 5;

    private final SpotRepository spotRepository;
    private final SpotTranslationRepository translationRepository;
    private final SpotImageRepository imageRepository;
    private final SpotMoodRepository moodRepository;
    private final BookmarkRepository bookmarkRepository;
    private final BookmarkQueryRepository bookmarkQueryRepository;
    private final SpotDetailQueryRepository spotDetailQueryRepository;

    public SpotDetailReader(SpotRepository spotRepository,
                            SpotTranslationRepository translationRepository,
                            SpotImageRepository imageRepository,
                            SpotMoodRepository moodRepository,
                            BookmarkRepository bookmarkRepository,
                            BookmarkQueryRepository bookmarkQueryRepository,
                            SpotDetailQueryRepository spotDetailQueryRepository) {
        this.spotRepository = spotRepository;
        this.translationRepository = translationRepository;
        this.imageRepository = imageRepository;
        this.moodRepository = moodRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.bookmarkQueryRepository = bookmarkQueryRepository;
        this.spotDetailQueryRepository = spotDetailQueryRepository;
    }

    public SpotDetailSnapshot read(Long spotId, @Nullable UUID memberId) {
        Spot spot = spotRepository.findById(spotId)
                .filter(s -> s.getStatus() == SpotStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPOT_NOT_FOUND));

        SpotTranslation translation = translationRepository.findBySpotIdAndLocale(spotId, DEFAULT_LOCALE)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPOT_NOT_FOUND));

        String addr1Ko = translationRepository.findBySpotIdAndLocale(spotId, KOREAN_LOCALE)
                .map(SpotTranslation::getAddr1)
                .orElse(null);

        List<SpotImage> images = imageRepository.findBySpotId(spotId);

        List<MoodTag> rawMoodTags = moodRepository.findBySpotId(spotId)
                .map(SpotMood::getMoodTags)
                .orElse(List.of());

        List<String> moodTags = rawMoodTags.stream()
                .map(MoodTag::getDisplayTag)
                .toList();

        List<String> moodTagKeys = rawMoodTags.stream()
                .map(MoodTag::getKey)
                .toList();

        long bookmarkCount = bookmarkQueryRepository.countBySpotId(spotId);

        boolean bookmarked = memberId != null
                && bookmarkRepository.existsByMemberIdAndSpotId(memberId, spotId);

        List<SpotImageItem> imageItems = images.stream()
                .map(img -> new SpotImageItem(img.getImageUrl(), img.isPrimary(), img.getSortOrder()))
                .toList();

        List<SimilarMoodSpotItem> similarMoodSpots =
                spotDetailQueryRepository.findSimilarMoodSpots(spotId, moodTagKeys, SIMILAR_MOOD_LIMIT);

        List<PopularAreaSpotItem> popularAreaSpots =
                spotDetailQueryRepository.findPopularSpotsByArea(spotId, spot.getArea(), spot.getDistrict(), POPULAR_AREA_LIMIT);

        String kakaoMapUrl = buildKakaoMapUrl(translation.getTitle(), spot.getLatitude(), spot.getLongitude());

        return new SpotDetailSnapshot(
                spot.getId(),
                translation.getTitle(),
                RegionDictionary.translateArea(spot.getArea()),
                RegionDictionary.translateDistrict(spot.getDistrict()),
                translation.getOverview(),
                spot.getHomepage(),
                spot.getTel(),
                spot.getContentType(),
                moodTags,
                moodTagKeys,
                imageItems,
                bookmarkCount,
                bookmarked,
                spot.getLatitude(),
                spot.getLongitude(),
                translation.getAddr1(),
                translation.getAddr2(),
                addr1Ko,
                kakaoMapUrl,
                similarMoodSpots,
                popularAreaSpots,
                null
        );
    }

    private String buildKakaoMapUrl(String title, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20");
        return KAKAO_MAP_URL_TEMPLATE.formatted(encodedTitle, latitude, longitude);
    }
}
