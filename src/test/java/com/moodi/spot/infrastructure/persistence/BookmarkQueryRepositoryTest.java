package com.moodi.spot.infrastructure.persistence;

import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberStatus;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.support.PostgresTestSupport;
import com.moodi.spot.application.dto.BookmarkSpotRow;
import com.moodi.spot.domain.Bookmark;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.support.MoodVectorFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkQueryRepositoryTest extends PostgresTestSupport {

    @Autowired
    private EntityManager em;

    private BookmarkQueryRepositoryImpl bookmarkQueryRepository;

    private UUID memberA;
    private UUID memberB;

    @BeforeEach
    void setUp() {
        bookmarkQueryRepository = new BookmarkQueryRepositoryImpl(em);
        memberA = insertMember();
        memberB = insertMember();
    }

    @Test
    @DisplayName("최신순 조회 - 현재 사용자의 북마크만 조회된다")
    void find_latest_only_own_bookmarks() {
        Long spot1 = insertPublishedSpot("서울");
        Long spot2 = insertPublishedSpot("서울");

        insertBookmark(memberA, spot1, LocalDateTime.of(2026, 7, 25, 10, 0));
        insertBookmark(memberB, spot2, LocalDateTime.of(2026, 7, 25, 11, 0));

        List<BookmarkSpotRow> rows = bookmarkQueryRepository.findByMemberLatest(
                memberA, null, null, null, null, 20);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).spotId()).isEqualTo(spot1);
    }

    @Test
    @DisplayName("최신순 조회 - created_at DESC, id DESC 정렬")
    void find_latest_ordered_by_created_at_desc() {
        Long spot1 = insertPublishedSpot("서울");
        Long spot2 = insertPublishedSpot("서울");
        Long spot3 = insertPublishedSpot("서울");

        insertBookmark(memberA, spot1, LocalDateTime.of(2026, 7, 23, 10, 0));
        insertBookmark(memberA, spot2, LocalDateTime.of(2026, 7, 25, 10, 0));
        insertBookmark(memberA, spot3, LocalDateTime.of(2026, 7, 24, 10, 0));

        List<BookmarkSpotRow> rows = bookmarkQueryRepository.findByMemberLatest(
                memberA, null, null, null, null, 20);

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).spotId()).isEqualTo(spot2);
        assertThat(rows.get(1).spotId()).isEqualTo(spot3);
        assertThat(rows.get(2).spotId()).isEqualTo(spot1);
    }

    @Test
    @DisplayName("최신순 커서 - 동일 created_at에서도 중복/누락이 없다")
    void find_latest_cursor_no_duplicate_on_same_created_at() {
        Long spot1 = insertPublishedSpot("서울");
        Long spot2 = insertPublishedSpot("서울");
        Long spot3 = insertPublishedSpot("서울");

        LocalDateTime sameTime = LocalDateTime.of(2026, 7, 25, 10, 0);
        insertBookmark(memberA, spot1, sameTime);
        insertBookmark(memberA, spot2, sameTime);
        insertBookmark(memberA, spot3, sameTime);

        // 1페이지: size=2 → size+1=3건 조회
        List<BookmarkSpotRow> page1 = bookmarkQueryRepository.findByMemberLatest(
                memberA, null, null, null, null, 2);
        assertThat(page1).hasSize(3);

        List<BookmarkSpotRow> page1Items = page1.subList(0, 2);
        BookmarkSpotRow lastItem = page1Items.getLast();

        // 2페이지: 커서 기반
        List<BookmarkSpotRow> page2 = bookmarkQueryRepository.findByMemberLatest(
                memberA, null, null, lastItem.bookmarkId(), lastItem.bookmarkedAt(), 2);

        assertThat(page2).hasSize(1);
        // 전체 spotId 합집합이 3개여야 중복/누락 없음
        Set<Long> allSpotIds = new HashSet<>();
        page1Items.forEach(r -> allSpotIds.add(r.spotId()));
        page2.forEach(r -> allSpotIds.add(r.spotId()));
        assertThat(allSpotIds).hasSize(3);
    }

    @Test
    @DisplayName("지역 필터 - area 정확 일치")
    void find_latest_area_filter() {
        Long seoulSpot = insertPublishedSpot("서울");
        Long busanSpot = insertPublishedSpot("부산");

        insertBookmark(memberA, seoulSpot, LocalDateTime.of(2026, 7, 25, 10, 0));
        insertBookmark(memberA, busanSpot, LocalDateTime.of(2026, 7, 25, 11, 0));

        List<BookmarkSpotRow> rows = bookmarkQueryRepository.findByMemberLatest(
                memberA, "서울", null, null, null, 20);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).area()).isEqualTo("서울");
    }

    @Test
    @DisplayName("무드 태그 필터 - OR 필터링")
    void find_latest_mood_tag_filter() {
        Long spot1 = insertPublishedSpot("서울");
        Long spot2 = insertPublishedSpot("서울");
        Long spot3 = insertPublishedSpot("서울");

        insertSpotMood(spot1, List.of(MoodTag.RETRO, MoodTag.COZY));
        insertSpotMood(spot2, List.of(MoodTag.MODERN));
        insertSpotMood(spot3, List.of(MoodTag.COZY, MoodTag.SERENE));

        insertBookmark(memberA, spot1, LocalDateTime.of(2026, 7, 25, 10, 0));
        insertBookmark(memberA, spot2, LocalDateTime.of(2026, 7, 25, 11, 0));
        insertBookmark(memberA, spot3, LocalDateTime.of(2026, 7, 25, 12, 0));

        // retro OR cozy → spot1, spot3
        List<BookmarkSpotRow> rows = bookmarkQueryRepository.findByMemberLatest(
                memberA, null, List.of("retro", "cozy"), null, null, 20);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(BookmarkSpotRow::spotId)
                .containsExactlyInAnyOrder(spot1, spot3);
    }

    @Test
    @DisplayName("무드 태그 필터 - 태그가 여러 개인 스팟이 중복 노출되지 않는다")
    void find_latest_mood_tag_filter_no_duplicate() {
        Long spot1 = insertPublishedSpot("서울");
        insertSpotMood(spot1, List.of(MoodTag.RETRO, MoodTag.COZY));
        insertBookmark(memberA, spot1, LocalDateTime.of(2026, 7, 25, 10, 0));

        // retro OR cozy로 필터해도 spot1은 1번만 나와야 함
        List<BookmarkSpotRow> rows = bookmarkQueryRepository.findByMemberLatest(
                memberA, null, List.of("retro", "cozy"), null, null, 20);

        assertThat(rows).hasSize(1);
    }

    @Test
    @DisplayName("인기순 조회 - bookmark_count DESC, spot_id DESC 정렬")
    void find_popular_ordered_by_bookmark_count_desc() {
        Long spot1 = insertPublishedSpot("서울");
        Long spot2 = insertPublishedSpot("서울");
        Long spot3 = insertPublishedSpot("서울");

        insertBookmark(memberA, spot1, LocalDateTime.of(2026, 7, 25, 10, 0));
        insertBookmark(memberA, spot2, LocalDateTime.of(2026, 7, 25, 11, 0));
        insertBookmark(memberA, spot3, LocalDateTime.of(2026, 7, 25, 12, 0));
        insertBookmark(memberB, spot2, LocalDateTime.of(2026, 7, 25, 10, 0));
        insertBookmark(memberB, spot1, LocalDateTime.of(2026, 7, 25, 11, 0));

        List<BookmarkSpotRow> rows = bookmarkQueryRepository.findByMemberPopular(
                memberA, null, null, null, null, 20);

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).spotId()).isEqualTo(spot2); // count=2, spotId 더 큼
        assertThat(rows.get(0).bookmarkCount()).isEqualTo(2);
        assertThat(rows.get(1).spotId()).isEqualTo(spot1); // count=2
        assertThat(rows.get(2).spotId()).isEqualTo(spot3); // count=1
    }

    @Test
    @DisplayName("인기순 커서 - 동일 bookmark_count에서 spotId DESC로 순서 고정")
    void find_popular_cursor_same_count() {
        Long spot1 = insertPublishedSpot("서울");
        Long spot2 = insertPublishedSpot("서울");
        Long spot3 = insertPublishedSpot("서울");

        insertBookmark(memberA, spot1, LocalDateTime.of(2026, 7, 25, 10, 0));
        insertBookmark(memberA, spot2, LocalDateTime.of(2026, 7, 25, 11, 0));
        insertBookmark(memberA, spot3, LocalDateTime.of(2026, 7, 25, 12, 0));

        // 1페이지: size=2
        List<BookmarkSpotRow> page1 = bookmarkQueryRepository.findByMemberPopular(
                memberA, null, null, null, null, 2);

        List<BookmarkSpotRow> page1Items = page1.subList(0, 2);
        BookmarkSpotRow lastItem = page1Items.getLast();

        // 2페이지
        List<BookmarkSpotRow> page2 = bookmarkQueryRepository.findByMemberPopular(
                memberA, null, null, lastItem.spotId(), lastItem.bookmarkCount(), 2);

        assertThat(page2).hasSize(1);
        Set<Long> allSpotIds = new HashSet<>();
        page1Items.forEach(r -> allSpotIds.add(r.spotId()));
        page2.forEach(r -> allSpotIds.add(r.spotId()));
        assertThat(allSpotIds).hasSize(3);
    }

    @Test
    @DisplayName("단건 북마크 수가 정확하다")
    void count_by_spot_id() {
        Long spotId = insertPublishedSpot("서울");
        insertBookmark(memberA, spotId, LocalDateTime.of(2026, 7, 25, 10, 0));
        insertBookmark(memberB, spotId, LocalDateTime.of(2026, 7, 25, 11, 0));

        long count = bookmarkQueryRepository.countBySpotId(spotId);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("복수 스팟 북마크 수가 한 번의 집계 쿼리로 조회된다")
    void count_by_spot_ids() {
        Long spot1 = insertPublishedSpot("서울");
        Long spot2 = insertPublishedSpot("서울");
        Long spot3 = insertPublishedSpot("서울");

        insertBookmark(memberA, spot1, LocalDateTime.of(2026, 7, 25, 10, 0));
        insertBookmark(memberB, spot1, LocalDateTime.of(2026, 7, 25, 11, 0));
        insertBookmark(memberA, spot2, LocalDateTime.of(2026, 7, 25, 12, 0));

        Map<Long, Long> counts = bookmarkQueryRepository.countBySpotIds(List.of(spot1, spot2, spot3));

        assertThat(counts.get(spot1)).isEqualTo(2);
        assertThat(counts.get(spot2)).isEqualTo(1);
        assertThat(counts.getOrDefault(spot3, 0L)).isEqualTo(0L);
    }

    @Test
    @DisplayName("복수 스팟 저장 여부 일괄 조회")
    void find_bookmarked_spot_ids() {
        Long spot1 = insertPublishedSpot("서울");
        Long spot2 = insertPublishedSpot("서울");
        Long spot3 = insertPublishedSpot("서울");

        insertBookmark(memberA, spot1, LocalDateTime.of(2026, 7, 25, 10, 0));
        insertBookmark(memberA, spot3, LocalDateTime.of(2026, 7, 25, 11, 0));

        Set<Long> bookmarkedIds = bookmarkQueryRepository.findBookmarkedSpotIds(
                memberA, List.of(spot1, spot2, spot3));

        assertThat(bookmarkedIds).containsExactlyInAnyOrder(spot1, spot3);
    }

    @Test
    @DisplayName("일괄 삭제 - 현재 사용자의 북마크만 삭제된다")
    void delete_only_own_bookmarks() {
        Long spot1 = insertPublishedSpot("서울");
        Long spot2 = insertPublishedSpot("서울");

        insertBookmark(memberA, spot1, LocalDateTime.of(2026, 7, 25, 10, 0));
        insertBookmark(memberA, spot2, LocalDateTime.of(2026, 7, 25, 11, 0));
        insertBookmark(memberB, spot1, LocalDateTime.of(2026, 7, 25, 12, 0));

        int deleted = bookmarkQueryRepository.deleteByMemberIdAndSpotIds(memberA, List.of(spot1, spot2));

        assertThat(deleted).isEqualTo(2);
        assertThat(bookmarkQueryRepository.countBySpotId(spot1)).isEqualTo(1);
    }

    @Test
    @DisplayName("일괄 삭제 - 이미 삭제된 스팟이 포함되어도 정상 처리된다")
    void delete_idempotent() {
        Long spot1 = insertPublishedSpot("서울");
        Long spot2 = insertPublishedSpot("서울");

        insertBookmark(memberA, spot1, LocalDateTime.of(2026, 7, 25, 10, 0));

        int deleted = bookmarkQueryRepository.deleteByMemberIdAndSpotIds(memberA, List.of(spot1, spot2));

        assertThat(deleted).isEqualTo(1);
    }

    // --- 헬퍼 메서드 ---

    private UUID insertMember() {
        Member member = Member.create(OAuthProvider.GOOGLE, "sub-" + UUID.randomUUID(), UUID.randomUUID() + "@test.com");
        ReflectionTestUtils.setField(member, "status", MemberStatus.ACTIVE);
        em.persist(member);
        em.flush();
        return member.getId();
    }

    private Long insertPublishedSpot(String area) {
        Spot spot = Spot.create("content-" + UUID.randomUUID(), SpotContentType.TOURIST_ATTRACTION,
                area, null, null, "kor_service", 126.0, 37.0, null, null, null, null, null);
        spot.publish();
        em.persist(spot);
        em.flush();
        return spot.getId();
    }

    private Long insertBookmark(UUID memberId, Long spotId, LocalDateTime createdAt) {
        Bookmark bookmark = Bookmark.create(memberId, spotId);
        em.persist(bookmark);
        em.flush();
        // native UPDATE로 createdAt 덮어쓰기 (JPA Auditing 우회)
        em.createNativeQuery("UPDATE bookmark SET created_at = :ca, updated_at = :ca WHERE id = :id")
                .setParameter("ca", createdAt)
                .setParameter("id", bookmark.getId())
                .executeUpdate();
        em.clear();
        return bookmark.getId();
    }

    private void insertSpotMood(Long spotId, List<MoodTag> tags) {
        SpotMood spotMood = SpotMood.create(spotId, MoodVectorFixture.create(), tags, 0.9);
        em.persist(spotMood);
        em.flush();
    }
}
