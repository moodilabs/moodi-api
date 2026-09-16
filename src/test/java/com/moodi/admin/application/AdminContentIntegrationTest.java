package com.moodi.admin.application;

import com.moodi.shared.support.RepositoryTestSupport;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.mood.MoodTag;
import com.moodi.route.application.RecommendedRouteService;
import com.moodi.route.domain.RecommendedRouteRepository;
import com.moodi.discovery.application.RecommendedAreaService;
import com.moodi.discovery.domain.PickAreaLevel;
import com.moodi.member.application.SurveyImageService;
import com.moodi.member.application.AgreementPolicyReader;
import com.moodi.member.application.MemberOnboardingService;
import com.moodi.member.application.dto.AgreementCommand;
import com.moodi.member.domain.*;
import com.moodi.member.support.MemberFixture;
import com.moodi.spot.domain.*;
import com.moodi.spot.support.SpotFixture;
import com.moodi.support.application.PolicyAdminService;
import com.moodi.support.application.PolicyQueryService;
import com.moodi.support.application.dto.PolicyCommand;
import com.moodi.support.domain.PolicyType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class AdminContentIntegrationTest extends RepositoryTestSupport {
    @Autowired RecommendedRouteService routes;
    @Autowired RecommendedRouteRepository routeRepository;
    @Autowired RecommendedAreaService areas;
    @Autowired SurveyImageService images;
    @Autowired SpotRepository spots;
    @Autowired SpotImageRepository spotImages;
    @Autowired PolicyAdminService policies;
    @Autowired PolicyQueryService policyQuery;
    @Autowired AgreementPolicyReader agreementPolicyReader;
    @Autowired MemberOnboardingService onboarding;
    @Autowired MemberRepository members;
    @Autowired MemberAgreementRepository agreements;
    @Autowired EntityManager em;

    private Spot publishedSpot(String code) {
        Spot spot = SpotFixture.create(code, "kor_service");
        spot.publish();
        return spots.save(spot);
    }

    private RecommendedRouteService.Command route(Long spotId, int order, boolean visible) {
        return new RecommendedRouteService.Command("Seoul walk", "https://example.com/seoul.jpg", "Seoul", visible, order,
                List.of(new RecommendedRouteService.Command.Stop(spotId, 1, 1)));
    }

    @Test @DisplayName("운영 루트는 노출 순서대로 3개만 반환하고 숨김 스팟과 삭제된 루트를 제외한다")
    void route_lifecycle_and_publication() {
        Spot spot = publishedSpot("curation-route");
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 4; i++) ids.add(routes.create(route(spot.getId(), i, true)));
        Long hidden = routes.create(route(spot.getId(), 0, false));
        em.flush(); em.clear();
        assertThat(routes.getPublished()).extracting(RecommendedRouteService.View::id).containsExactlyElementsOf(ids.subList(0, 3));
        List<Long> reversed = List.of(hidden, ids.get(3), ids.get(2), ids.get(1), ids.get(0));
        routes.reorder(reversed);
        assertThat(routes.getPublished()).extracting(RecommendedRouteService.View::id).containsExactly(ids.get(3), ids.get(2), ids.get(1));
        routes.changeVisibility(hidden, true);
        assertThat(routes.getPublished().getFirst().id()).isEqualTo(hidden);
        routes.update(hidden, route(spot.getId(), 0, true));
        em.flush(); em.clear();
        assertThat(routes.get(hidden).stops()).hasSize(1);
        assertThatThrownBy(() -> routes.reorder(List.of(hidden, hidden))).isInstanceOf(BusinessException.class);
        routes.delete(hidden); em.flush(); em.clear();
        assertThat(routeRepository.findById(hidden)).isEmpty();
        assertThat(((Number) em.createNativeQuery("SELECT COUNT(*) FROM recommended_route_stop WHERE recommended_route_id = :id")
                .setParameter("id", hidden).getSingleResult()).longValue()).isZero();
        Spot persisted = spots.findById(spot.getId()).orElseThrow();
        persisted.hide("unavailable", LocalDateTime.now());
        em.flush(); em.clear();
        assertThat(routes.getPublished()).isEmpty();
        assertThatThrownBy(() -> routes.create(route(spot.getId(), 0, true))).isInstanceOf(BusinessException.class);
    }

    @Test @DisplayName("추천 지역은 실제 스팟이 있는 자동완성 값만 등록하고 수정·삭제할 수 있다")
    void recommended_area_lifecycle() {
        Spot spot = publishedSpot("curation-area");
        Long id = areas.create(new RecommendedAreaService.Command(PickAreaLevel.REGION, "Seoul", null, null, "Seoul", 0));
        areas.update(id, new RecommendedAreaService.Command(PickAreaLevel.DISTRICT, "Seoul", "Jongno-gu", null, "Jongno", 1));
        em.flush(); em.clear();
        assertThat(areas.getPublished()).extracting(RecommendedAreaService.View::label).containsExactly("Jongno");
        assertThatThrownBy(() -> areas.create(new RecommendedAreaService.Command(PickAreaLevel.REGION, "Unknown", null, null, "Unknown", 0)))
                .isInstanceOf(BusinessException.class);
        spots.findById(spot.getId()).orElseThrow().hide("hidden", LocalDateTime.now());
        em.flush();
        assertThat(areas.getPublished()).isEmpty();
        areas.delete(id);
        assertThat(areas.getAll()).isEmpty();
    }

    @Test @DisplayName("사전조사 이미지는 선택 스팟의 이미지만 허용하고 교체·삭제·비노출이 반영된다")
    void survey_image_lifecycle() {
        Spot spot = publishedSpot("curation-survey");
        String first = "https://example.com/first.jpg", second = "https://example.com/second.jpg";
        spotImages.save(SpotImage.createPrimary(spot.getId(), first));
        spotImages.save(SpotImage.createPrimary(spot.getId(), second));
        em.flush();
        Long id = images.create(new SurveyImageService.Command(MoodTag.NATURE, spot.getId(), first, 0));
        images.update(id, new SurveyImageService.Command(MoodTag.SERENE, spot.getId(), second, 0));
        em.flush(); em.clear();
        assertThat(images.getPublished().getFirst().imageUrl()).isEqualTo(second);
        assertThatThrownBy(() -> images.create(new SurveyImageService.Command(MoodTag.NATURE, spot.getId(), "https://example.com/unowned.jpg", 0)))
                .isInstanceOf(BusinessException.class);
        spots.findById(spot.getId()).orElseThrow().hide("hidden", LocalDateTime.now());
        em.flush();
        assertThat(images.getPublished()).isEmpty();
        images.delete(id);
        assertThat(images.getAll()).isEmpty();
    }

    @Test @DisplayName("약관은 언어별 현재 공개 시행본만 조회하고 과거 버전 및 동의 버전을 보존한다")
    void localized_policies_and_consent_snapshot() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Long english = policies.create(new PolicyCommand(PolicyType.TERMS_OF_SERVICE, "1.0", "English", yesterday, "en-US", true, true));
        Long korean = policies.create(new PolicyCommand(PolicyType.TERMS_OF_SERVICE, "1.0", "국문", yesterday, "ko-KR", true, true));
        policies.create(new PolicyCommand(PolicyType.TERMS_OF_SERVICE, "2.0", "미래", LocalDate.now().plusDays(5), "ko-KR", true, true));
        Long hidden = policies.create(new PolicyCommand(PolicyType.TERMS_OF_SERVICE, "3.0", "숨김", yesterday, "ko-KR", true, false));
        policies.create(new PolicyCommand(PolicyType.TERMS_OF_SERVICE, "4.0", "시행 중지", yesterday, "ko-KR", false, true));
        em.flush(); em.clear();
        assertThat(policyQuery.getCurrentPolicy(PolicyType.TERMS_OF_SERVICE).id()).isEqualTo(english);
        assertThat(policyQuery.getCurrentPolicy(PolicyType.TERMS_OF_SERVICE, "ko-KR").id()).isEqualTo(korean);
        assertThat(agreementPolicyReader.findCurrent(AgreementType.TERMS_OF_SERVICE, "ko-KR").id()).isEqualTo(korean);
        Member member = MemberFixture.create();
        member.updateProfile("consent_user", "KR", 1996, Gender.FEMALE, LocalDate.now().getYear());
        members.save(member);
        onboarding.agree(member.getId(), new AgreementCommand(Map.of(AgreementType.TERMS_OF_SERVICE, true,
                AgreementType.PRIVACY_POLICY, true, AgreementType.AGE_OVER_14, true, AgreementType.MARKETING, false),
                "ko-KR", Map.of(AgreementType.TERMS_OF_SERVICE, korean)));
        policies.changePublication(hidden, true, true);
        policies.changePublication(korean, false, false);
        em.flush(); em.clear();
        MemberAgreement consent = agreements.findByMemberId(member.getId()).stream().filter(a -> a.getType() == AgreementType.TERMS_OF_SERVICE).findFirst().orElseThrow();
        assertThat(consent.getPolicyId()).isEqualTo(korean);
        assertThat(consent.getPolicyVersion()).isEqualTo("1.0");
        assertThat(consent.getPolicyLocale()).isEqualTo("ko-KR");
        assertThat(consent.getAgreedAt()).isNotNull();
        assertThat(policyQuery.getCurrentPolicy(PolicyType.TERMS_OF_SERVICE, "ko-KR").id()).isEqualTo(hidden);
        assertThat(policies.get(korean).content()).isEqualTo("국문");
        assertThatThrownBy(() -> policies.delete(korean)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policies.update(korean, new PolicyCommand(PolicyType.TERMS_OF_SERVICE, "1.0", "수정", yesterday, "ko-KR", false, false)))
                .isInstanceOf(BusinessException.class);
    }
}
