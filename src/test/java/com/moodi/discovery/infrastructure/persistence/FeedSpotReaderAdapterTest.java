package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.FeedCursor;
import com.moodi.discovery.application.FeedQuery;
import com.moodi.discovery.application.FeedSpotRow;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberStatus;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.support.PostgresTestSupport;
import com.moodi.spot.domain.Bookmark;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.support.MoodVectorFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 무드 jsonb 필터는 H2에서 재현되지 않으므로 Postgres에서 검증한다.
 */
class FeedSpotReaderAdapterTest extends PostgresTestSupport {

    private static final LocalDateTime SESSION_AT = LocalDateTime.of(2026, 8, 3, 12, 0);
    private static final int IMPRESSION_WINDOW_DAYS = 30;
    private static final String SEED = "seed-a";

    @Autowired
    private EntityManager em;

    private FeedSpotReaderAdapter adapter;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        adapter = new FeedSpotReaderAdapter(em);
        memberId = insertMember();
    }

    @Test
    @DisplayName("개인화 피드 - 선호 무드와 겹치는 스팟만 조회된다")
    void personalized_feed_filters_by_mood_intersection() {
        Long matching = insertPublishedSpot();
        Long notMatching = insertPublishedSpot();
        insertSpotMood(matching, List.of(MoodTag.COZY));
        insertSpotMood(notMatching, List.of(MoodTag.NEON));

        List<FeedSpotRow> rows = adapter.readPersonalizedFeed(
                memberQuery(List.of(MoodTag.COZY, MoodTag.SERENE), null, 20));

        assertThat(rows).extracting(FeedSpotRow::spotId).containsExactly(matching);
    }

    @Test
    @DisplayName("개인화 피드 - 무드 일치 수가 많은 스팟이 앞에 온다")
    void personalized_feed_orders_by_match_count_desc() {
        Long twoMatches = insertPublishedSpot();
        Long oneMatch = insertPublishedSpot();
        insertSpotMood(twoMatches, List.of(MoodTag.COZY, MoodTag.SERENE));
        insertSpotMood(oneMatch, List.of(MoodTag.COZY, MoodTag.NEON));

        List<FeedSpotRow> rows = adapter.readPersonalizedFeed(
                memberQuery(List.of(MoodTag.COZY, MoodTag.SERENE), null, 20));

        assertThat(rows).extracting(FeedSpotRow::spotId).containsExactly(twoMatches, oneMatch);
        assertThat(rows.get(0).rankScore()).isEqualTo(2);
        assertThat(rows.get(1).rankScore()).isEqualTo(1);
    }

    @Test
    @DisplayName("개인화 피드 - 최근 본 스팟은 안 본 스팟보다 뒤로 밀린다")
    void personalized_feed_puts_seen_spots_last() {
        Long seen = insertPublishedSpot();
        Long unseen = insertPublishedSpot();
        insertSpotMood(seen, List.of(MoodTag.COZY, MoodTag.SERENE));
        insertSpotMood(unseen, List.of(MoodTag.COZY));
        insertImpression(seen, SESSION_AT.minusDays(1));

        List<FeedSpotRow> rows = adapter.readPersonalizedFeed(
                memberQuery(List.of(MoodTag.COZY, MoodTag.SERENE), null, 20));

        // 무드 일치 수는 seen 쪽이 더 높지만 "안 본 것 우선"이 앞선 정렬 키다
        assertThat(rows).extracting(FeedSpotRow::spotId).containsExactly(unseen, seen);
        assertThat(rows.get(0).seen()).isZero();
        assertThat(rows.get(1).seen()).isEqualTo(1);
    }

    @Test
    @DisplayName("개인화 피드 - 유효 기간이 지난 노출 이력은 다시 안 본 것으로 취급된다")
    void personalized_feed_ignores_expired_impressions() {
        Long spotId = insertPublishedSpot();
        insertSpotMood(spotId, List.of(MoodTag.COZY));
        insertImpression(spotId, SESSION_AT.minusDays(IMPRESSION_WINDOW_DAYS + 1));

        List<FeedSpotRow> rows = adapter.readPersonalizedFeed(
                memberQuery(List.of(MoodTag.COZY), null, 20));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).seen()).isZero();
    }

    @Test
    @DisplayName("개인화 피드 - 세션 시작 이후에 기록된 노출은 이번 스크롤의 순서를 바꾸지 않는다")
    void personalized_feed_ignores_impressions_after_session_start() {
        Long spotId = insertPublishedSpot();
        insertSpotMood(spotId, List.of(MoodTag.COZY));
        // 1페이지 응답 직후 기록된 노출 이력
        insertImpression(spotId, SESSION_AT.plusSeconds(1));

        List<FeedSpotRow> rows = adapter.readPersonalizedFeed(
                memberQuery(List.of(MoodTag.COZY), null, 20));

        assertThat(rows.get(0).seen()).isZero();
    }

    @Test
    @DisplayName("개인화 피드 - 커서로 페이지를 넘겨도 중복·누락이 없다")
    void personalized_feed_cursor_has_no_duplicates_or_gaps() {
        List<Long> spotIds = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            Long spotId = insertPublishedSpot();
            insertSpotMood(spotId, List.of(MoodTag.COZY));
            spotIds.add(spotId);
        }

        Set<Long> collected = new HashSet<>();
        FeedCursor cursor = null;
        for (int page = 0; page < 3; page++) {
            List<FeedSpotRow> rows = adapter.readPersonalizedFeed(
                    memberQuery(List.of(MoodTag.COZY), cursor, 10));
            if (rows.isEmpty()) {
                break;
            }
            rows.forEach(row -> collected.add(row.spotId()));
            cursor = toCursor(rows.getLast());
        }

        assertThat(collected).containsExactlyInAnyOrderElementsOf(spotIds);
    }

    @Test
    @DisplayName("개인화 피드 - 비활성 스팟은 제외된다")
    void personalized_feed_excludes_unpublished_spots() {
        Long published = insertPublishedSpot();
        Long pending = insertUnpublishedSpot();
        insertSpotMood(published, List.of(MoodTag.COZY));
        insertSpotMood(pending, List.of(MoodTag.COZY));

        List<FeedSpotRow> rows = adapter.readPersonalizedFeed(
                memberQuery(List.of(MoodTag.COZY), null, 20));

        assertThat(rows).extracting(FeedSpotRow::spotId).containsExactly(published);
    }

    @Test
    @DisplayName("개인화 피드 - 선호 무드가 없으면 무드 필터 없이 전체 스팟을 준다")
    void personalized_feed_without_preferred_moods_returns_all_spots() {
        Long withMood = insertPublishedSpot();
        Long withoutMood = insertPublishedSpot();
        insertSpotMood(withMood, List.of(MoodTag.NEON));

        List<FeedSpotRow> rows = adapter.readPersonalizedFeed(memberQuery(List.of(), null, 20));

        assertThat(rows).extracting(FeedSpotRow::spotId)
                .containsExactlyInAnyOrder(withMood, withoutMood);
        assertThat(rows).allMatch(row -> row.rankScore() == 0);
    }

    @Test
    @DisplayName("개인화 피드 - 북마크 여부가 함께 조회된다")
    void personalized_feed_marks_bookmarked_spots() {
        Long bookmarked = insertPublishedSpot();
        Long notBookmarked = insertPublishedSpot();
        insertSpotMood(bookmarked, List.of(MoodTag.COZY));
        insertSpotMood(notBookmarked, List.of(MoodTag.COZY));
        insertBookmark(memberId, bookmarked);

        List<FeedSpotRow> rows = adapter.readPersonalizedFeed(
                memberQuery(List.of(MoodTag.COZY), null, 20));

        assertThat(rows).filteredOn(FeedSpotRow::bookmarked)
                .extracting(FeedSpotRow::spotId).containsExactly(bookmarked);
    }

    @Test
    @DisplayName("개인화 피드 - 대표 이미지가 여러 장이어도 스팟이 중복되지 않는다")
    void personalized_feed_does_not_duplicate_spot_with_multiple_primary_images() {
        Long spotId = insertPublishedSpot();
        insertSpotMood(spotId, List.of(MoodTag.COZY));
        em.persist(SpotImage.createPrimary(spotId, "https://img/extra"));
        em.flush();

        List<FeedSpotRow> rows = adapter.readPersonalizedFeed(
                memberQuery(List.of(MoodTag.COZY), null, 20));

        assertThat(rows).hasSize(1);
    }

    @Test
    @DisplayName("비회원 피드 - 북마크 수가 많은 스팟이 앞에 온다")
    void guest_feed_orders_by_bookmark_count_desc() {
        Long popular = insertPublishedSpot();
        Long unpopular = insertPublishedSpot();
        UUID otherMember = insertMember();
        insertBookmark(memberId, popular);
        insertBookmark(otherMember, popular);
        insertBookmark(memberId, unpopular);

        List<FeedSpotRow> rows = adapter.readGuestFeed(guestQuery(SEED, null, 20));

        assertThat(rows).extracting(FeedSpotRow::spotId).containsExactly(popular, unpopular);
        assertThat(rows.get(0).rankScore()).isEqualTo(2);
    }

    @Test
    @DisplayName("비회원 피드 - 무드 태그가 없는 스팟도 포함되고 북마크 여부는 항상 false다")
    void guest_feed_includes_all_spots_and_never_bookmarked() {
        Long withMood = insertPublishedSpot();
        Long withoutMood = insertPublishedSpot();
        insertSpotMood(withMood, List.of(MoodTag.COZY));
        insertBookmark(memberId, withMood);

        List<FeedSpotRow> rows = adapter.readGuestFeed(guestQuery(SEED, null, 20));

        assertThat(rows).extracting(FeedSpotRow::spotId)
                .containsExactlyInAnyOrder(withMood, withoutMood);
        assertThat(rows).noneMatch(FeedSpotRow::bookmarked);
    }

    @Test
    @DisplayName("비회원 피드 - 시드가 바뀌면 순서가 다시 섞인다")
    void guest_feed_reshuffles_on_new_seed() {
        for (int i = 0; i < 30; i++) {
            insertPublishedSpot();
        }

        List<Long> firstOrder = toSpotIds(adapter.readGuestFeed(guestQuery("seed-a", null, 30)));
        List<Long> secondOrder = toSpotIds(adapter.readGuestFeed(guestQuery("seed-b", null, 30)));

        assertThat(firstOrder).containsExactlyInAnyOrderElementsOf(secondOrder);
        assertThat(firstOrder).isNotEqualTo(secondOrder);
    }

    @Test
    @DisplayName("비회원 피드 - 같은 시드면 순서가 고정된다")
    void guest_feed_keeps_order_for_same_seed() {
        for (int i = 0; i < 10; i++) {
            insertPublishedSpot();
        }

        List<Long> firstCall = toSpotIds(adapter.readGuestFeed(guestQuery(SEED, null, 10)));
        List<Long> secondCall = toSpotIds(adapter.readGuestFeed(guestQuery(SEED, null, 10)));

        assertThat(firstCall).isEqualTo(secondCall);
    }

    @Test
    @DisplayName("비회원 피드 - 커서로 페이지를 넘겨도 중복·누락이 없다")
    void guest_feed_cursor_has_no_duplicates_or_gaps() {
        List<Long> spotIds = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            spotIds.add(insertPublishedSpot());
        }

        Set<Long> collected = new HashSet<>();
        FeedCursor cursor = null;
        for (int page = 0; page < 3; page++) {
            List<FeedSpotRow> rows = adapter.readGuestFeed(guestQuery(SEED, cursor, 10));
            if (rows.isEmpty()) {
                break;
            }
            rows.forEach(row -> collected.add(row.spotId()));
            cursor = toCursor(rows.getLast());
        }

        assertThat(collected).containsExactlyInAnyOrderElementsOf(spotIds);
    }

    // --- 헬퍼 ---

    private FeedQuery memberQuery(List<MoodTag> moods, FeedCursor cursor, int limit) {
        return new FeedQuery(
                memberId,
                moods.stream().map(MoodTag::getKey).toList(),
                SEED,
                SESSION_AT,
                SESSION_AT.minusDays(IMPRESSION_WINDOW_DAYS),
                cursor,
                limit
        );
    }

    private FeedQuery guestQuery(String seed, FeedCursor cursor, int limit) {
        return new FeedQuery(null, List.of(), seed, SESSION_AT,
                SESSION_AT.minusDays(IMPRESSION_WINDOW_DAYS), cursor, limit);
    }

    private FeedCursor toCursor(FeedSpotRow row) {
        return new FeedCursor(SEED, SESSION_AT, row.seen(), row.rankScore(),
                row.shuffleKey(), row.spotId());
    }

    private List<Long> toSpotIds(List<FeedSpotRow> rows) {
        return rows.stream().map(FeedSpotRow::spotId).toList();
    }

    @Test
    @DisplayName("지역명은 영문으로 조회된다")
    void feed_returns_english_area() {
        Long spotId = insertPublishedSpot();
        insertSpotMood(spotId, List.of(MoodTag.COZY));

        List<FeedSpotRow> rows = adapter.readPersonalizedFeed(
                memberQuery(List.of(MoodTag.COZY), null, 20));

        assertThat(rows).singleElement().extracting(FeedSpotRow::area).isEqualTo("Seoul");
    }

    private UUID insertMember() {
        Member member = Member.create(OAuthProvider.GOOGLE, "sub-" + UUID.randomUUID(),
                UUID.randomUUID() + "@test.com");
        ReflectionTestUtils.setField(member, "status", MemberStatus.ACTIVE);
        em.persist(member);
        em.flush();
        return member.getId();
    }

    private Long insertPublishedSpot() {
        Spot spot = createSpot();
        spot.publish();
        return persistSpotWithDetails(spot);
    }

    private Long insertUnpublishedSpot() {
        return persistSpotWithDetails(createSpot());
    }

    private Spot createSpot() {
        return Spot.create("content-" + UUID.randomUUID(), SpotContentType.TOURIST_ATTRACTION,
                "서울", null, null, "kor_service", 126.0, 37.0, null, null, null, null, null);
    }

    private Long persistSpotWithDetails(Spot spot) {
        em.persist(spot);
        em.flush();
        em.persist(SpotTranslation.create(spot.getId(), "en-US", "스팟-" + spot.getId(),
                null, null, null));
        em.persist(SpotImage.createPrimary(spot.getId(), "https://img/" + spot.getId()));
        em.flush();
        return spot.getId();
    }

    private void insertSpotMood(Long spotId, List<MoodTag> tags) {
        em.persist(SpotMood.create(spotId, MoodVectorFixture.create(), tags, 0.9));
        em.flush();
    }

    private void insertBookmark(UUID bookmarkOwner, Long spotId) {
        em.persist(Bookmark.create(bookmarkOwner, spotId));
        em.flush();
    }

    private void insertImpression(Long spotId, LocalDateTime shownAt) {
        em.createNativeQuery("""
                INSERT INTO feed_impression (member_id, spot_id, shown_at)
                VALUES (:memberId, :spotId, :shownAt)
                """)
                .setParameter("memberId", memberId)
                .setParameter("spotId", spotId)
                .setParameter("shownAt", shownAt)
                .executeUpdate();
    }
}
