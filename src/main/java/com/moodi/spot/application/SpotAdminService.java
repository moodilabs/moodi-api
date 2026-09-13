package com.moodi.spot.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.response.CursorResponse;
import com.moodi.spot.application.dto.SpotAdminDetail;
import com.moodi.spot.application.dto.SpotAdminFilter;
import com.moodi.spot.application.dto.SpotAdminRow;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotDescription;
import com.moodi.spot.domain.SpotDescriptionRepository;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotStatus;
import com.moodi.spot.domain.SpotTranslationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 어드민의 스팟 관리(`ADM-F06`). 목록·상세·노출 상태·루트 제외·무드 태그 보정·AI 설명 수정.
 * TourAPI 재동기화와 설명 재생성은 배치 프로필(`spot-pipeline` 등)로 돌리며 여기서 트리거하지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class SpotAdminService {

    private static final String LOCALE = "en-US";

    private final SpotRepository spotRepository;
    private final SpotAdminQueryRepository spotAdminQueryRepository;
    private final SpotTranslationRepository spotTranslationRepository;
    private final SpotDescriptionRepository spotDescriptionRepository;
    private final SpotMoodRepository spotMoodRepository;
    private final SpotImageRepository spotImageRepository;
    private final BookmarkQueryRepository bookmarkQueryRepository;
    private final Clock clock;

    public SpotAdminService(
            SpotRepository spotRepository,
            SpotAdminQueryRepository spotAdminQueryRepository,
            SpotTranslationRepository spotTranslationRepository,
            SpotDescriptionRepository spotDescriptionRepository,
            SpotMoodRepository spotMoodRepository,
            SpotImageRepository spotImageRepository,
            BookmarkQueryRepository bookmarkQueryRepository,
            Clock clock
    ) {
        this.spotRepository = spotRepository;
        this.spotAdminQueryRepository = spotAdminQueryRepository;
        this.spotTranslationRepository = spotTranslationRepository;
        this.spotDescriptionRepository = spotDescriptionRepository;
        this.spotMoodRepository = spotMoodRepository;
        this.spotImageRepository = spotImageRepository;
        this.bookmarkQueryRepository = bookmarkQueryRepository;
        this.clock = clock;
    }

    public CursorResponse<SpotAdminRow> getSpots(SpotAdminFilter filter, Long cursorId, int size) {
        List<SpotAdminRow> rows = spotAdminQueryRepository.findAll(filter, cursorId, size + 1);
        boolean hasNext = rows.size() > size;
        List<SpotAdminRow> page = hasNext ? rows.subList(0, size) : rows;
        if (page.isEmpty()) {
            return CursorResponse.empty();
        }
        return CursorResponse.of(page, hasNext ? String.valueOf(page.getLast().id()) : null, hasNext);
    }

    public SpotAdminDetail getSpot(Long spotId) {
        Spot spot = findSpot(spotId);
        SpotAdminDetail.Translation translation = spotTranslationRepository.findBySpotIdAndLocale(spotId, LOCALE)
                .map(found -> new SpotAdminDetail.Translation(found.getTitle(), found.getOverview(),
                        found.getAddr1(), found.getAddr2()))
                .orElse(null);
        String description = spotDescriptionRepository.findBySpotIdAndLocale(spotId, LOCALE)
                .map(SpotDescription::getContent)
                .orElse(null);
        SpotAdminDetail.Mood mood = spotMoodRepository.findBySpotId(spotId)
                .map(found -> new SpotAdminDetail.Mood(found.getMoodTags(), found.getConfidence()))
                .orElse(null);
        List<SpotAdminDetail.Image> images = spotImageRepository.findBySpotId(spotId).stream()
                .sorted(java.util.Comparator.comparingInt(SpotImage::getSortOrder))
                .map(image -> new SpotAdminDetail.Image(image.getImageUrl(), image.isPrimary(), image.getSortOrder()))
                .toList();
        return new SpotAdminDetail(
                spot.getId(), spot.getContentId(), spot.getSource(), spot.getContentType(), spot.getArea(),
                spot.getDistrict(), spot.getNeighborhood(), spot.getLatitude(), spot.getLongitude(), spot.getTel(),
                spot.getHomepage(), spot.getStatus(), spot.getStatusReason(), spot.getStatusChangedAt(),
                spot.isRouteExcluded(), translation, description, mood, images,
                bookmarkQueryRepository.countBySpotId(spotId), spot.getCreatedAt(), spot.getUpdatedAt());
    }

    /**
     * 노출 상태 전환. `PUBLISHED`(숨김 해제) · `HIDDEN`(숨김, 사유 필수) · `DELETED`(삭제, 사유 필수).
     * `TAGGING_PENDING`으로는 되돌릴 수 없다 — 그건 배치의 영역이다.
     */
    @Transactional
    public void changeStatus(Long spotId, SpotStatus status, String reason) {
        Spot spot = findSpot(spotId);
        LocalDateTime now = LocalDateTime.now(clock);
        switch (status) {
            case PUBLISHED -> spot.unhide(now);
            case HIDDEN -> spot.hide(reason, now);
            case DELETED -> spot.markDeleted(reason, now);
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        spotRepository.save(spot);
    }

    @Transactional
    public void changeRouteExclusion(Long spotId, boolean excluded) {
        Spot spot = findSpot(spotId);
        spot.changeRouteExclusion(excluded);
        spotRepository.save(spot);
    }

    /** 무드 태깅이 안 된 스팟(`TAGGING_PENDING`)은 보정할 대상이 없다. */
    @Transactional
    public void overrideMoodTags(Long spotId, List<MoodTag> moodTags) {
        findSpot(spotId);
        SpotMood spotMood = spotMoodRepository.findBySpotId(spotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPOT_MOOD_NOT_FOUND));
        spotMood.overrideTags(moodTags);
        spotMoodRepository.save(spotMood);
    }

    /** 설명이 아직 없으면 새로 만든다 — 배치가 못 만든 스팟을 사람이 채울 수 있게. */
    @Transactional
    public void updateDescription(Long spotId, String content) {
        findSpot(spotId);
        SpotDescription description = spotDescriptionRepository.findBySpotIdAndLocale(spotId, LOCALE)
                .orElseGet(() -> SpotDescription.create(spotId, LOCALE, content));
        description.updateContent(content);
        spotDescriptionRepository.save(description);
    }

    private Spot findSpot(Long spotId) {
        return spotRepository.findById(spotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPOT_NOT_FOUND));
    }
}
