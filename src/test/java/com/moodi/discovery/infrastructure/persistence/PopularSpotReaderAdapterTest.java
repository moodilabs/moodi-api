package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.PopularSpotRow;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberStatus;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.shared.support.PostgresTestSupport;
import com.moodi.spot.domain.Bookmark;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotTranslation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PopularSpotReaderAdapterTest extends PostgresTestSupport {

    private static final int TOP_LIMIT = 5;

    @Autowired
    private EntityManager em;

    private PopularSpotReaderAdapter adapter;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        adapter = new PopularSpotReaderAdapter(em);
        memberId = insertMember();
    }

    @Test
    @DisplayName("북마크 수 내림차순으로 최대 5개만 조회된다")
    void read_top_five_by_bookmark_count() {
        List<Long> spotIds = List.of(insertPublishedSpot(), insertPublishedSpot(), insertPublishedSpot(),
                insertPublishedSpot(), insertPublishedSpot(), insertPublishedSpot());
        // 6번째 스팟이 가장 인기 있고, 나머지는 순서대로 북마크가 하나씩 적다
        for (int i = 0; i < spotIds.size(); i++) {
            addBookmarks(spotIds.get(i), i);
        }

        List<PopularSpotRow> rows = adapter.readTopByBookmarkCount(memberId, TOP_LIMIT);

        assertThat(rows).hasSize(TOP_LIMIT);
        assertThat(rows).extracting(PopularSpotRow::bookmarkCount)
                .containsExactly(5L, 4L, 3L, 2L, 1L);
    }

    @Test
    @DisplayName("북마크 수가 같으면 spot_id 오름차순으로 고정된다")
    void read_breaks_tie_by_spot_id() {
        Long first = insertPublishedSpot();
        Long second = insertPublishedSpot();

        List<PopularSpotRow> rows = adapter.readTopByBookmarkCount(memberId, TOP_LIMIT);

        assertThat(rows).extracting(PopularSpotRow::spotId).containsExactly(first, second);
    }

    @Test
    @DisplayName("비활성 스팟은 제외된다")
    void read_excludes_unpublished_spots() {
        Long published = insertPublishedSpot();
        insertUnpublishedSpot();

        List<PopularSpotRow> rows = adapter.readTopByBookmarkCount(memberId, TOP_LIMIT);

        assertThat(rows).extracting(PopularSpotRow::spotId).containsExactly(published);
    }

    @Test
    @DisplayName("조회한 회원의 북마크 여부가 함께 조회된다")
    void read_marks_bookmarked_spots() {
        Long bookmarked = insertPublishedSpot();
        Long notBookmarked = insertPublishedSpot();
        em.persist(Bookmark.create(memberId, bookmarked));
        em.flush();

        List<PopularSpotRow> rows = adapter.readTopByBookmarkCount(memberId, TOP_LIMIT);

        assertThat(rows).filteredOn(PopularSpotRow::bookmarked)
                .extracting(PopularSpotRow::spotId).containsExactly(bookmarked);
        assertThat(rows).extracting(PopularSpotRow::spotId).contains(notBookmarked);
    }

    private void addBookmarks(Long spotId, int count) {
        for (int i = 0; i < count; i++) {
            em.persist(Bookmark.create(insertMember(), spotId));
        }
        em.flush();
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
        em.persist(SpotTranslation.create(spot.getId(), "ko-KR", "스팟-" + spot.getId(),
                null, null, null));
        em.persist(SpotImage.createPrimary(spot.getId(), "https://img/" + spot.getId()));
        em.flush();
        return spot.getId();
    }
}
