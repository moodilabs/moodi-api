package com.moodi.discovery.infrastructure.persistence;

import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberStatus;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.shared.support.PostgresTestSupport;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FeedImpressionRepositoryImplTest extends PostgresTestSupport {

    private static final LocalDateTime FIRST_SHOWN_AT = LocalDateTime.of(2026, 8, 1, 10, 0);
    private static final LocalDateTime SECOND_SHOWN_AT = LocalDateTime.of(2026, 8, 3, 12, 0);

    @Autowired
    private EntityManager em;

    private FeedImpressionRepositoryImpl repository;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        repository = new FeedImpressionRepositoryImpl(em);
        memberId = insertMember();
    }

    @Test
    @DisplayName("노출 이력이 없던 스팟은 새로 기록된다")
    void record_inserts_new_impressions() {
        List<Long> spotIds = List.of(insertSpot(), insertSpot());

        repository.recordShown(memberId, spotIds, FIRST_SHOWN_AT);

        assertThat(countImpressions()).isEqualTo(2);
    }

    @Test
    @DisplayName("이미 본 스팟은 행이 늘지 않고 shown_at만 갱신된다")
    void record_updates_shown_at_on_conflict() {
        Long spotId = insertSpot();
        repository.recordShown(memberId, List.of(spotId), FIRST_SHOWN_AT);

        repository.recordShown(memberId, List.of(spotId), SECOND_SHOWN_AT);

        assertThat(countImpressions()).isEqualTo(1);
        assertThat(findShownAt(spotId)).isEqualTo(SECOND_SHOWN_AT);
    }

    @Test
    @DisplayName("빈 목록이면 아무것도 기록되지 않는다")
    void record_ignores_empty_spot_ids() {
        repository.recordShown(memberId, List.of(), FIRST_SHOWN_AT);

        assertThat(countImpressions()).isZero();
    }

    @Test
    @DisplayName("같은 스팟이 중복으로 들어와도 upsert가 실패하지 않는다")
    void record_deduplicates_spot_ids() {
        Long spotId = insertSpot();

        repository.recordShown(memberId, List.of(spotId, spotId), FIRST_SHOWN_AT);

        assertThat(countImpressions()).isEqualTo(1);
        assertThat(findShownAt(spotId)).isEqualTo(FIRST_SHOWN_AT);
    }

    private long countImpressions() {
        Number count = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM feed_impression WHERE member_id = :memberId")
                .setParameter("memberId", memberId)
                .getSingleResult();
        return count.longValue();
    }

    private LocalDateTime findShownAt(Long spotId) {
        return (LocalDateTime) em.createNativeQuery(
                        "SELECT shown_at FROM feed_impression WHERE member_id = :memberId AND spot_id = :spotId")
                .setParameter("memberId", memberId)
                .setParameter("spotId", spotId)
                .getSingleResult();
    }

    private UUID insertMember() {
        Member member = Member.create(OAuthProvider.GOOGLE, "sub-" + UUID.randomUUID(),
                UUID.randomUUID() + "@test.com");
        ReflectionTestUtils.setField(member, "status", MemberStatus.ACTIVE);
        em.persist(member);
        em.flush();
        return member.getId();
    }

    private Long insertSpot() {
        Spot spot = Spot.create("content-" + UUID.randomUUID(), SpotContentType.TOURIST_ATTRACTION,
                "서울", null, null, "kor_service", 126.0, 37.0, null, null, null, null, null);
        spot.publish();
        em.persist(spot);
        em.flush();
        return spot.getId();
    }
}
