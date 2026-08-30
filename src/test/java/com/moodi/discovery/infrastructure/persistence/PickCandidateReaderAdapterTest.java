package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.PickCandidate;
import com.moodi.discovery.domain.PickArea;
import com.moodi.discovery.domain.PickAreaLevel;
import com.moodi.discovery.domain.PickAreas;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberStatus;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.support.PostgresTestSupport;
import com.moodi.spot.domain.Bookmark;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotDescription;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 무드 jsonb 조건과 지역 조합 필터는 H2에서 재현되지 않으므로 Postgres에서 검증한다.
 */
class PickCandidateReaderAdapterTest extends PostgresTestSupport {

    private static final int LIMIT = 500;

    @Autowired
    private EntityManager em;

    private PickCandidateReaderAdapter adapter;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        adapter = new PickCandidateReaderAdapter(em);
        memberId = insertMember();
    }

    @Test
    @DisplayName("시·도만 고르면 그 안의 모든 하위 지역 스팟이 후보가 된다")
    void read_by_region_includes_all_districts() {
        Long seongsu = insertSpot("서울", "성동구", "성수동");
        Long mapo = insertSpot("서울", "마포구", "연남동");
        Long busan = insertSpot("부산", "해운대구", "우동");

        List<PickCandidate> candidates = adapter.readByAreas(memberId, areas(
                new PickArea(PickAreaLevel.REGION, "서울", null, null)), LIMIT);

        assertThat(candidates).extracting(PickCandidate::spotId).containsExactlyInAnyOrder(seongsu, mapo);
        assertThat(candidates).extracting(PickCandidate::spotId).doesNotContain(busan);
    }

    @Test
    @DisplayName("구까지 고르면 그 구의 스팟만 후보가 된다")
    void read_by_district_narrows_to_district() {
        Long seongsu = insertSpot("서울", "성동구", "성수동");
        insertSpot("서울", "마포구", "연남동");

        List<PickCandidate> candidates = adapter.readByAreas(memberId, areas(
                new PickArea(PickAreaLevel.DISTRICT, "서울", "성동구", null)), LIMIT);

        assertThat(candidates).extracting(PickCandidate::spotId).containsExactly(seongsu);
    }

    @Test
    @DisplayName("동까지 고르면 그 동의 스팟만 후보가 된다")
    void read_by_neighborhood_narrows_to_neighborhood() {
        Long seongsu = insertSpot("서울", "성동구", "성수동");
        insertSpot("서울", "성동구", "금호동");

        List<PickCandidate> candidates = adapter.readByAreas(memberId, areas(
                new PickArea(PickAreaLevel.NEIGHBORHOOD, "서울", "성동구", "성수동")), LIMIT);

        assertThat(candidates).extracting(PickCandidate::spotId).containsExactly(seongsu);
    }

    @Test
    @DisplayName("여러 지역을 고르면 그중 하나라도 걸리는 스팟이 후보가 된다")
    void read_by_multiple_areas_uses_or() {
        Long seongsu = insertSpot("서울", "성동구", "성수동");
        Long haeundae = insertSpot("부산", "해운대구", "우동");
        Long daegu = insertSpot("대구", "중구", "동성로");

        List<PickCandidate> candidates = adapter.readByAreas(memberId, areas(
                new PickArea(PickAreaLevel.DISTRICT, "서울", "성동구", null),
                new PickArea(PickAreaLevel.REGION, "부산", null, null)), LIMIT);

        assertThat(candidates).extracting(PickCandidate::spotId)
                .containsExactlyInAnyOrder(seongsu, haeundae)
                .doesNotContain(daegu);
    }

    @Test
    @DisplayName("무드 분석이 없는 스팟은 유사도를 잴 수 없어 후보에서 제외된다")
    void read_excludes_spot_without_mood() {
        Long withMood = insertSpot("서울", "성동구", "성수동");
        Long withoutMood = insertSpotWithoutMood("서울", "성동구", "성수동");

        List<PickCandidate> candidates = adapter.readByAreas(memberId, areas(
                new PickArea(PickAreaLevel.REGION, "서울", null, null)), LIMIT);

        assertThat(candidates).extracting(PickCandidate::spotId)
                .containsExactly(withMood)
                .doesNotContain(withoutMood);
    }

    @Test
    @DisplayName("비활성 스팟은 후보에서 제외된다")
    void read_excludes_unpublished_spot() {
        Long published = insertSpot("서울", "성동구", "성수동");
        insertUnpublishedSpot("서울", "성동구", "성수동");

        List<PickCandidate> candidates = adapter.readByAreas(memberId, areas(
                new PickArea(PickAreaLevel.REGION, "서울", null, null)), LIMIT);

        assertThat(candidates).extracting(PickCandidate::spotId).containsExactly(published);
    }

    @Test
    @DisplayName("무드 벡터·태그·주소·설명·좌표·저장 여부가 함께 조회된다")
    void read_maps_candidate_fields() {
        Long spotId = insertSpot("서울", "성동구", "성수동");
        em.persist(SpotDescription.create(spotId, "ko-KR", "붉은 벽돌 거리"));
        em.persist(Bookmark.create(memberId, spotId));
        em.flush();

        PickCandidate candidate = adapter.readByAreas(memberId, areas(
                new PickArea(PickAreaLevel.REGION, "서울", null, null)), LIMIT).getFirst();

        assertThat(candidate.moodVector()).isNotNull();
        assertThat(candidate.moodTags()).contains(MoodTag.COZY);
        assertThat(candidate.address()).isEqualTo("서울 성동구 성수동");
        assertThat(candidate.description()).isEqualTo("붉은 벽돌 거리");
        assertThat(candidate.latitude()).isEqualTo(37.0);
        assertThat(candidate.longitude()).isEqualTo(126.0);
        assertThat(candidate.bookmarked()).isTrue();
        assertThat(candidate.imageUrl()).isEqualTo("https://img/" + spotId);
    }

    @Test
    @DisplayName("대체 추천은 지역과 무관하게 무드 태그가 겹치는 스팟을 조회한다")
    void read_by_mood_tags_ignores_area() {
        Long busan = insertSpot("부산", "해운대구", "우동", List.of(MoodTag.COZY));
        Long daegu = insertSpot("대구", "중구", "동성로", List.of(MoodTag.NEON));

        List<PickCandidate> candidates = adapter.readByMoodTags(memberId, List.of(MoodTag.COZY), LIMIT);

        assertThat(candidates).extracting(PickCandidate::spotId)
                .containsExactly(busan)
                .doesNotContain(daegu);
    }

    @Test
    @DisplayName("대체 추천에서 태그가 비어 있으면 조회하지 않는다")
    void read_by_mood_tags_returns_empty_for_no_tags() {
        insertSpot("서울", "성동구", "성수동");

        assertThat(adapter.readByMoodTags(memberId, List.of(), LIMIT)).isEmpty();
    }

    @Test
    @DisplayName("후보 상한을 넘으면 상한만큼만 조회된다")
    void read_applies_candidate_limit() {
        insertSpot("서울", "성동구", "성수동");
        insertSpot("서울", "성동구", "성수동");
        insertSpot("서울", "성동구", "성수동");

        List<PickCandidate> candidates = adapter.readByAreas(memberId, areas(
                new PickArea(PickAreaLevel.REGION, "서울", null, null)), 2);

        assertThat(candidates).hasSize(2);
    }

    private PickAreas areas(PickArea... areas) {
        return PickAreas.of(List.of(areas));
    }

    private UUID insertMember() {
        Member member = Member.create(OAuthProvider.GOOGLE, "sub-" + UUID.randomUUID(),
                UUID.randomUUID() + "@test.com");
        ReflectionTestUtils.setField(member, "status", MemberStatus.ACTIVE);
        em.persist(member);
        em.flush();
        return member.getId();
    }

    private Long insertSpot(String area, String district, String neighborhood) {
        return insertSpot(area, district, neighborhood, List.of(MoodTag.COZY));
    }

    private Long insertSpot(String area, String district, String neighborhood, List<MoodTag> tags) {
        Long spotId = insertSpotWithoutMood(area, district, neighborhood);
        em.persist(SpotMood.create(spotId, MoodVectorFixture.create(), tags, 0.9));
        em.flush();
        return spotId;
    }

    private Long insertSpotWithoutMood(String area, String district, String neighborhood) {
        Spot spot = createSpot(area, district, neighborhood);
        spot.publish();
        return persistSpotWithDetails(spot, area, district, neighborhood);
    }

    private Long insertUnpublishedSpot(String area, String district, String neighborhood) {
        return persistSpotWithDetails(createSpot(area, district, neighborhood), area, district, neighborhood);
    }

    private Spot createSpot(String area, String district, String neighborhood) {
        return Spot.create("content-" + UUID.randomUUID(), SpotContentType.TOURIST_ATTRACTION,
                area, district, neighborhood, "kor_service", 126.0, 37.0, null, null, null, null, null);
    }

    private Long persistSpotWithDetails(Spot spot, String area, String district, String neighborhood) {
        em.persist(spot);
        em.flush();
        em.persist(SpotTranslation.create(spot.getId(), "ko-KR", "스팟-" + spot.getId(), null,
                area + " " + district + " " + neighborhood, null));
        em.persist(SpotImage.createPrimary(spot.getId(), "https://img/" + spot.getId()));
        em.flush();
        return spot.getId();
    }
}
