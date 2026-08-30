package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.domain.PickArea;
import com.moodi.discovery.domain.PickAreaLevel;
import com.moodi.discovery.domain.PickRequest;
import com.moodi.discovery.domain.PickRequestArea;
import com.moodi.discovery.domain.PickRequestAreaRepository;
import com.moodi.discovery.domain.PickRequestRepository;
import com.moodi.discovery.domain.PickResultSpot;
import com.moodi.discovery.domain.PickResultSpotRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * orm.xml 매핑이 실제 스키마와 맞는지 확인한다. {@code rank}·{@code level}·{@code fallback}처럼
 * SQL 키워드와 겹치는 컬럼명이 있어 매핑이 어긋나면 저장 시점에야 드러난다.
 */
class PickRequestPersistenceTest extends PostgresTestSupport {

    @Autowired
    private EntityManager em;
    @Autowired
    private PickRequestRepository pickRequestRepository;
    @Autowired
    private PickRequestAreaRepository pickRequestAreaRepository;
    @Autowired
    private PickResultSpotRepository pickResultSpotRepository;

    private UUID memberId;

    @BeforeEach
    void setUp() {
        memberId = insertMember();
    }

    @Test
    @DisplayName("추천 요청이 저장되고 다시 조회된다")
    void save_and_find_pick_request() {
        PickRequest saved = pickRequestRepository.save(PickRequest.create(memberId, "picks/a.jpg"));
        em.flush();
        em.clear();

        PickRequest found = pickRequestRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getMemberId()).isEqualTo(memberId);
        assertThat(found.getImageKey()).isEqualTo("picks/a.jpg");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("선택 지역이 고른 순서대로 저장되고 조회된다")
    void save_and_find_areas_in_order() {
        PickRequest request = pickRequestRepository.save(PickRequest.create(memberId, "picks/a.jpg"));
        pickRequestAreaRepository.save(PickRequestArea.of(request.getId(),
                new PickArea(PickAreaLevel.NEIGHBORHOOD, "서울", "성동구", "성수동"), 0));
        pickRequestAreaRepository.save(PickRequestArea.of(request.getId(),
                new PickArea(PickAreaLevel.REGION, "부산", null, null), 1));
        em.flush();
        em.clear();

        List<PickRequestArea> areas =
                pickRequestAreaRepository.findByPickRequestIdOrderBySortOrder(request.getId());

        assertThat(areas).hasSize(2);
        assertThat(areas.get(0).toArea())
                .isEqualTo(new PickArea(PickAreaLevel.NEIGHBORHOOD, "서울", "성동구", "성수동"));
        assertThat(areas.get(1).getLevel()).isEqualTo(PickAreaLevel.REGION);
        assertThat(areas.get(1).getDistrict()).isNull();
    }

    @Test
    @DisplayName("추천 결과가 순위·유사도·대체 여부와 함께 저장된다")
    void save_and_find_results() {
        PickRequest request = pickRequestRepository.save(PickRequest.create(memberId, "picks/a.jpg"));
        Long spotId = insertSpot();
        pickResultSpotRepository.save(PickResultSpot.of(request.getId(), spotId, 0, 0.92, false));
        pickResultSpotRepository.save(PickResultSpot.of(request.getId(), spotId, 1, 0.81, true));
        em.flush();
        em.clear();

        List<PickResultSpot> results =
                pickResultSpotRepository.findByPickRequestIdOrderByRank(request.getId());

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getRank()).isZero();
        assertThat(results.get(0).getSimilarity()).isEqualTo(0.92);
        assertThat(results.get(0).isFallback()).isFalse();
        assertThat(results.get(1).isFallback()).isTrue();
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
                "서울", "성동구", "성수동", "kor_service", 126.0, 37.0, null, null, null, null, null);
        spot.publish();
        em.persist(spot);
        em.flush();
        return spot.getId();
    }
}
