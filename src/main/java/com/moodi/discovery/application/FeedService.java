package com.moodi.discovery.application;

import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.response.CursorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FeedService {

    private static final int PAGE_SIZE = 20;
    private static final int POPULAR_SPOT_LIMIT = 5;

    private final FeedSpotReader feedSpotReader;
    private final PopularSpotReader popularSpotReader;
    private final PreferredMoodReader preferredMoodReader;
    private final FeedImpressionRepository feedImpressionRepository;
    private final FeedSeedGenerator seedGenerator;
    private final FeedProperties feedProperties;
    private final Clock clock;

    public FeedService(FeedSpotReader feedSpotReader,
                       PopularSpotReader popularSpotReader,
                       PreferredMoodReader preferredMoodReader,
                       FeedImpressionRepository feedImpressionRepository,
                       FeedSeedGenerator seedGenerator,
                       FeedProperties feedProperties,
                       Clock clock) {
        this.feedSpotReader = feedSpotReader;
        this.popularSpotReader = popularSpotReader;
        this.preferredMoodReader = preferredMoodReader;
        this.feedImpressionRepository = feedImpressionRepository;
        this.seedGenerator = seedGenerator;
        this.feedProperties = feedProperties;
        this.clock = clock;
    }

    /**
     * 조회 API지만 응답에 나간 스팟의 노출 이력을 기록하므로 readOnly가 아니다.
     */
    @Transactional
    public CursorResponse<FeedSpotItem> getFeed(UUID memberId, String encodedCursor) {
        FeedQuery query = toQuery(memberId, encodedCursor);
        List<FeedSpotRow> rows = memberId == null
                ? feedSpotReader.readGuestFeed(query)
                : feedSpotReader.readPersonalizedFeed(query);

        boolean hasNext = rows.size() > PAGE_SIZE;
        List<FeedSpotRow> pageRows = hasNext ? rows.subList(0, PAGE_SIZE) : rows;
        if (pageRows.isEmpty()) {
            return CursorResponse.empty();
        }

        recordImpressions(memberId, pageRows);

        List<FeedSpotItem> items = pageRows.stream().map(FeedSpotItem::from).toList();
        String nextCursor = hasNext ? toCursor(query, pageRows.getLast()).encode() : null;
        return CursorResponse.of(items, nextCursor, hasNext);
    }

    public List<PopularSpotItem> getPopularSpots(UUID memberId) {
        return popularSpotReader.readTopByBookmarkCount(memberId, POPULAR_SPOT_LIMIT).stream()
                .map(PopularSpotItem::from)
                .toList();
    }

    /**
     * 커서가 없으면 새로고침으로 보고 새 시드와 새 세션 시각을 발급한다.
     * 커서가 있으면 그 안의 시드·세션 시각을 이어 써서 스크롤 중 순서를 고정한다.
     */
    private FeedQuery toQuery(UUID memberId, String encodedCursor) {
        FeedCursor cursor = (encodedCursor == null || encodedCursor.isBlank())
                ? null
                : FeedCursor.decode(encodedCursor);
        String seed = cursor != null ? cursor.seed() : seedGenerator.generate();
        LocalDateTime sessionAt = cursor != null ? cursor.sessionAt() : LocalDateTime.now(clock);

        return new FeedQuery(
                memberId,
                readMoodTagKeys(memberId),
                seed,
                sessionAt,
                sessionAt.minusDays(feedProperties.impressionWindowDays()),
                cursor,
                PAGE_SIZE + 1
        );
    }

    private List<String> readMoodTagKeys(UUID memberId) {
        if (memberId == null) {
            return List.of();
        }
        return preferredMoodReader.readByMemberId(memberId).stream()
                .map(MoodTag::getKey)
                .toList();
    }

    private void recordImpressions(UUID memberId, List<FeedSpotRow> pageRows) {
        if (memberId == null) {
            return;
        }
        List<Long> spotIds = pageRows.stream().map(FeedSpotRow::spotId).toList();
        feedImpressionRepository.recordShown(memberId, spotIds, LocalDateTime.now(clock));
    }

    private FeedCursor toCursor(FeedQuery query, FeedSpotRow lastRow) {
        return new FeedCursor(
                query.seed(),
                query.sessionAt(),
                lastRow.seen(),
                lastRow.rankScore(),
                lastRow.shuffleKey(),
                lastRow.spotId()
        );
    }
}
