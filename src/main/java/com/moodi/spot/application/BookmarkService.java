package com.moodi.spot.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.response.CursorResponse;
import com.moodi.spot.domain.Bookmark;
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
import com.moodi.spot.application.dto.BookmarkSpotRow;
import com.moodi.spot.application.dto.BookmarkListRequest;
import com.moodi.spot.application.dto.BookmarkSortType;
import com.moodi.spot.application.dto.BookmarkSpotItem;
import com.moodi.spot.application.dto.BookmarkToggleResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final BookmarkQueryRepository bookmarkQueryRepository;
    private final SpotRepository spotRepository;
    private final SpotTranslationRepository spotTranslationRepository;
    private final SpotImageRepository spotImageRepository;
    private final SpotMoodRepository spotMoodRepository;

    public BookmarkService(
            BookmarkRepository bookmarkRepository,
            BookmarkQueryRepository bookmarkQueryRepository,
            SpotRepository spotRepository,
            SpotTranslationRepository spotTranslationRepository,
            SpotImageRepository spotImageRepository,
            SpotMoodRepository spotMoodRepository
    ) {
        this.bookmarkRepository = bookmarkRepository;
        this.bookmarkQueryRepository = bookmarkQueryRepository;
        this.spotRepository = spotRepository;
        this.spotTranslationRepository = spotTranslationRepository;
        this.spotImageRepository = spotImageRepository;
        this.spotMoodRepository = spotMoodRepository;
    }

    @Transactional
    public BookmarkToggleResult toggle(UUID memberId, Long spotId) {
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPOT_NOT_FOUND));

        Optional<Bookmark> existing = bookmarkRepository.findByMemberIdAndSpotId(memberId, spotId);

        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            bookmarkQueryRepository.flush();
            long count = bookmarkQueryRepository.countBySpotId(spotId);
            return new BookmarkToggleResult(spotId, false, count);
        }

        if (spot.getStatus() != SpotStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.SPOT_NOT_AVAILABLE);
        }

        try {
            bookmarkRepository.save(Bookmark.create(memberId, spotId));
            bookmarkQueryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 동시 중복 저장 시 유니크 제약 위반 → 이미 저장된 상태로 처리
            long count = bookmarkQueryRepository.countBySpotId(spotId);
            return new BookmarkToggleResult(spotId, true, count);
        }
        long count = bookmarkQueryRepository.countBySpotId(spotId);
        return new BookmarkToggleResult(spotId, true, count);
    }

    public CursorResponse<BookmarkSpotItem> getBookmarks(UUID memberId, BookmarkListRequest request) {
        List<String> moodTagKeys = request.moodTagKeys();
        List<BookmarkSpotRow> rows;

        if (request.sort() == BookmarkSortType.POPULAR) {
            rows = bookmarkQueryRepository.findByMemberPopular(
                    memberId, request.area(), moodTagKeys,
                    request.cursorSpotId(), request.cursorBookmarkCount(),
                    request.size());
        } else {
            rows = bookmarkQueryRepository.findByMemberLatest(
                    memberId, request.area(), moodTagKeys,
                    request.cursorId(), request.cursorCreatedAt(),
                    request.size());
        }

        boolean hasNext = rows.size() > request.size();
        List<BookmarkSpotRow> pageRows = hasNext ? rows.subList(0, request.size()) : rows;

        if (pageRows.isEmpty()) {
            return CursorResponse.empty();
        }

        List<Long> spotIds = pageRows.stream().map(BookmarkSpotRow::spotId).toList();

        Map<Long, SpotTranslation> translationMap = spotTranslationRepository.findBySpotIdIn(spotIds)
                .stream()
                .collect(Collectors.toMap(SpotTranslation::getSpotId, Function.identity(), (a, b) -> a));

        Map<Long, String> imageMap = spotImageRepository.findBySpotIdInAndIsPrimaryTrue(spotIds)
                .stream()
                .collect(Collectors.toMap(SpotImage::getSpotId, SpotImage::getImageUrl, (a, b) -> a));

        Map<Long, List<MoodTag>> moodTagMap = spotMoodRepository.findBySpotIdIn(spotIds)
                .stream()
                .collect(Collectors.toMap(SpotMood::getSpotId, SpotMood::getMoodTags, (a, b) -> a));

        List<BookmarkSpotItem> items = pageRows.stream()
                .map(row -> {
                    SpotTranslation translation = translationMap.get(row.spotId());
                    return new BookmarkSpotItem(
                            row.spotId(),
                            translation != null ? translation.getTitle() : null,
                            imageMap.get(row.spotId()),
                            row.area(),
                            moodTagMap.getOrDefault(row.spotId(), List.of()),
                            row.bookmarkCount(),
                            row.bookmarkedAt(),
                            true
                    );
                })
                .toList();

        String nextCursor = hasNext ? buildCursor(pageRows.getLast(), request.sort()) : null;

        return CursorResponse.of(items, nextCursor, hasNext);
    }

    @Transactional
    public int deleteBookmarks(UUID memberId, List<Long> spotIds) {
        return bookmarkQueryRepository.deleteByMemberIdAndSpotIds(memberId, spotIds);
    }

    private String buildCursor(BookmarkSpotRow lastRow, BookmarkSortType sort) {
        if (sort == BookmarkSortType.POPULAR) {
            return lastRow.bookmarkCount() + "," + lastRow.spotId();
        }
        return lastRow.bookmarkedAt().toString() + "," + lastRow.bookmarkId();
    }
}
