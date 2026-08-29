package com.moodi.spot.application;

import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.response.CursorResponse;
import com.moodi.spot.application.dto.SpotSearchItem;
import com.moodi.spot.application.dto.SpotSearchRequest;
import com.moodi.spot.application.dto.SpotSearchRow;
import com.moodi.spot.application.dto.SpotSearchSortType;
import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SpotSearchService {

    private static final String DESCRIPTION_LOCALE = "en-US";

    private final SpotSearchQueryRepository spotSearchQueryRepository;
    private final BookmarkQueryRepository bookmarkQueryRepository;
    private final SpotTranslationRepository spotTranslationRepository;
    private final SpotDescriptionRepository spotDescriptionRepository;
    private final SpotImageRepository spotImageRepository;
    private final SpotMoodRepository spotMoodRepository;

    public SpotSearchService(
            SpotSearchQueryRepository spotSearchQueryRepository,
            BookmarkQueryRepository bookmarkQueryRepository,
            SpotTranslationRepository spotTranslationRepository,
            SpotDescriptionRepository spotDescriptionRepository,
            SpotImageRepository spotImageRepository,
            SpotMoodRepository spotMoodRepository
    ) {
        this.spotSearchQueryRepository = spotSearchQueryRepository;
        this.bookmarkQueryRepository = bookmarkQueryRepository;
        this.spotTranslationRepository = spotTranslationRepository;
        this.spotDescriptionRepository = spotDescriptionRepository;
        this.spotImageRepository = spotImageRepository;
        this.spotMoodRepository = spotMoodRepository;
    }

    public CursorResponse<SpotSearchItem> search(UUID memberId, SpotSearchRequest request) {
        List<SpotSearchRow> rows = fetchRows(memberId, request);

        boolean hasNext = rows.size() > request.size();
        List<SpotSearchRow> pageRows = hasNext ? rows.subList(0, request.size()) : rows;

        if (pageRows.isEmpty()) {
            return CursorResponse.empty();
        }

        List<Long> spotIds = pageRows.stream().map(SpotSearchRow::spotId).toList();

        Map<Long, SpotTranslation> translationMap = spotTranslationRepository.findBySpotIdIn(spotIds)
                .stream()
                .collect(Collectors.toMap(SpotTranslation::getSpotId, Function.identity(), (a, b) -> a));

        Map<Long, String> descriptionMap = spotDescriptionRepository
                .findBySpotIdInAndLocale(spotIds, DESCRIPTION_LOCALE)
                .stream()
                .collect(Collectors.toMap(SpotDescription::getSpotId, SpotDescription::getContent, (a, b) -> a));

        Map<Long, String> imageMap = spotImageRepository.findBySpotIdInAndIsPrimaryTrue(spotIds)
                .stream()
                .collect(Collectors.toMap(SpotImage::getSpotId, SpotImage::getImageUrl, (a, b) -> a));

        Map<Long, List<MoodTag>> moodTagMap = spotMoodRepository.findBySpotIdIn(spotIds)
                .stream()
                .collect(Collectors.toMap(SpotMood::getSpotId, SpotMood::getMoodTags, (a, b) -> a));

        Set<Long> bookmarkedSpotIds = memberId != null
                ? bookmarkQueryRepository.findBookmarkedSpotIds(memberId, spotIds)
                : Set.of();

        Set<Long> inRouteSpotIds = request.routePublicId() != null
                ? spotSearchQueryRepository.findSpotIdsInRoute(request.routePublicId(), spotIds)
                : Set.of();

        List<SpotSearchItem> items = pageRows.stream()
                .map(row -> {
                    SpotTranslation translation = translationMap.get(row.spotId());
                    return new SpotSearchItem(
                            row.spotId(),
                            translation != null ? translation.getTitle() : null,
                            imageMap.get(row.spotId()),
                            row.area(),
                            row.district(),
                            descriptionMap.get(row.spotId()),
                            moodTagMap.getOrDefault(row.spotId(), List.of()),
                            row.bookmarkCount(),
                            bookmarkedSpotIds.contains(row.spotId()),
                            inRouteSpotIds.contains(row.spotId())
                    );
                })
                .toList();

        String nextCursor = hasNext ? buildCursor(pageRows.getLast(), request) : null;

        return CursorResponse.of(items, nextCursor, hasNext);
    }

    private List<SpotSearchRow> fetchRows(UUID memberId, SpotSearchRequest request) {
        boolean hasKeyword = request.keyword() != null && !request.keyword().isBlank();

        if (hasKeyword && request.sort() == SpotSearchSortType.BEST_MATCH) {
            return spotSearchQueryRepository.searchByBestMatch(
                    request.keyword(), request.area(), request.moodTagKeys(),
                    request.saved(), memberId,
                    request.cursorMatchRank(), request.cursorBookmarkCount(),
                    request.cursorSpotId(), request.size());
        }

        return spotSearchQueryRepository.searchByMostSaved(
                request.area(), request.moodTagKeys(),
                request.saved(), memberId,
                request.cursorSpotId(), request.cursorBookmarkCount(), request.size());
    }

    private String buildCursor(SpotSearchRow lastRow, SpotSearchRequest request) {
        boolean hasKeyword = request.keyword() != null && !request.keyword().isBlank();
        if (hasKeyword && request.sort() == SpotSearchSortType.BEST_MATCH) {
            return lastRow.matchRank() + "," + lastRow.bookmarkCount() + "," + lastRow.spotId();
        }
        return lastRow.bookmarkCount() + "," + lastRow.spotId();
    }
}
