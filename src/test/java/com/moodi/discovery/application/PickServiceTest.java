package com.moodi.discovery.application;

import com.moodi.discovery.domain.PickArea;
import com.moodi.discovery.domain.PickAreaLevel;
import com.moodi.discovery.domain.PickAreas;
import com.moodi.discovery.domain.PickRequest;
import com.moodi.discovery.domain.PickRequestArea;
import com.moodi.discovery.domain.PickRequestAreaRepository;
import com.moodi.discovery.domain.PickRequestRepository;
import com.moodi.discovery.domain.PickResultSpot;
import com.moodi.discovery.domain.PickResultSpotRepository;
import com.moodi.discovery.support.MoodVectorFixture;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.mood.Atmosphere;
import com.moodi.shared.mood.Color;
import com.moodi.shared.mood.Era;
import com.moodi.shared.mood.Lighting;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.mood.MoodTagRuleEngine;
import com.moodi.shared.mood.MoodVector;
import com.moodi.shared.mood.Space;
import com.moodi.shared.mood.Structure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PickServiceTest {

    private static final int CANDIDATE_LIMIT = 500;
    private static final String IMAGE_KEY = "picks/member/photo.jpg";
    private static final PickArea SEOUL = new PickArea(PickAreaLevel.REGION, "서울", null, null);

    @Mock
    private MoodAnalysisClient moodAnalysisClient;
    @Mock
    private ImageStorageClient imageStorageClient;
    @Mock
    private PickCandidateReader pickCandidateReader;
    @Mock
    private PickRequestRepository pickRequestRepository;
    @Mock
    private PickRequestAreaRepository pickRequestAreaRepository;
    @Mock
    private PickResultSpotRepository pickResultSpotRepository;

    private PickService pickService;
    private final UUID memberId = UUID.randomUUID();
    private final UUID pickId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        pickService = new PickService(moodAnalysisClient, imageStorageClient, pickCandidateReader,
                pickRequestRepository, pickRequestAreaRepository, pickResultSpotRepository,
                new MoodTagRuleEngine(), new PickProperties(CANDIDATE_LIMIT));
    }

    @Test
    @DisplayName("업로드 사진과 무드가 비슷한 순서로 최대 5개를 추천한다")
    void recommend_orders_by_similarity_and_limits_to_five() {
        givenPickRequestSaved();
        given(imageStorageClient.issueReadUrl(IMAGE_KEY)).willReturn("https://read");
        given(moodAnalysisClient.analyze("https://read")).willReturn(MoodVectorFixture.serene());
        given(pickCandidateReader.readByAreas(any(), any(PickAreas.class), anyInt()))
                .willReturn(List.of(
                        candidate(1L, MoodVectorFixture.lively()),
                        candidate(2L, MoodVectorFixture.serene()),
                        candidate(3L, partiallySerene()),
                        candidate(4L, MoodVectorFixture.lively()),
                        candidate(5L, MoodVectorFixture.lively()),
                        candidate(6L, MoodVectorFixture.lively())));

        PickResult result = pickService.recommend(memberId, IMAGE_KEY, List.of(SEOUL));

        assertThat(result.spots()).hasSize(5);
        assertThat(result.spots().get(0).spotId()).isEqualTo(2L);
        assertThat(result.spots().get(1).spotId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("무드 벡터가 없는 스팟은 유사도를 잴 수 없어 후보에서 빠진다")
    void recommend_excludes_candidate_without_mood_vector() {
        givenPickRequestSaved();
        givenAnalyzed(MoodVectorFixture.serene());
        given(pickCandidateReader.readByAreas(any(), any(PickAreas.class), anyInt()))
                .willReturn(List.of(candidate(1L, null), candidate(2L, MoodVectorFixture.serene())));

        PickResult result = pickService.recommend(memberId, IMAGE_KEY, List.of(SEOUL));

        assertThat(result.spots()).extracting(PickResultItem::spotId).containsExactly(2L);
    }

    @Test
    @DisplayName("무드 태그는 최대 3개까지만 내보낸다")
    void recommend_limits_mood_tags_to_three() {
        givenPickRequestSaved();
        givenAnalyzed(MoodVectorFixture.serene());
        given(pickCandidateReader.readByAreas(any(), any(PickAreas.class), anyInt()))
                .willReturn(List.of(candidate(1L, MoodVectorFixture.serene(),
                        List.of(MoodTag.NATURE, MoodTag.OCEAN, MoodTag.CITYSCAPE, MoodTag.RIVERSIDE))));

        PickResult result = pickService.recommend(memberId, IMAGE_KEY, List.of(SEOUL));

        assertThat(result.spots().getFirst().moodTags()).hasSize(3);
    }

    @Test
    @DisplayName("지역 안에 결과가 없으면 지역 조건을 풀어 대체 추천을 내려준다")
    void recommend_falls_back_when_area_result_is_empty() {
        givenPickRequestSaved();
        givenAnalyzed(MoodVectorFixture.serene());
        given(pickCandidateReader.readByAreas(any(), any(PickAreas.class), anyInt())).willReturn(List.of());
        given(pickCandidateReader.readByMoodTags(any(), anyList(), anyInt()))
                .willReturn(List.of(candidate(9L, MoodVectorFixture.serene())));

        PickResult result = pickService.recommend(memberId, IMAGE_KEY, List.of(SEOUL));

        assertThat(result.spots()).isEmpty();
        assertThat(result.fallbackSpots()).extracting(PickResultItem::spotId).containsExactly(9L);
    }

    @Test
    @DisplayName("지역 안에서 결과를 찾으면 대체 추천은 조회하지 않는다")
    void recommend_skips_fallback_when_area_result_exists() {
        givenPickRequestSaved();
        givenAnalyzed(MoodVectorFixture.serene());
        given(pickCandidateReader.readByAreas(any(), any(PickAreas.class), anyInt()))
                .willReturn(List.of(candidate(1L, MoodVectorFixture.serene())));

        PickResult result = pickService.recommend(memberId, IMAGE_KEY, List.of(SEOUL));

        assertThat(result.fallbackSpots()).isEmpty();
        verify(pickCandidateReader, never()).readByMoodTags(any(), anyList(), anyInt());
    }

    @Test
    @DisplayName("추천 결과가 하나도 없어도 오류가 아니라 빈 결과를 응답한다")
    void recommend_returns_empty_result_without_error() {
        givenPickRequestSaved();
        givenAnalyzed(MoodVectorFixture.serene());
        given(pickCandidateReader.readByAreas(any(), any(PickAreas.class), anyInt())).willReturn(List.of());
        given(pickCandidateReader.readByMoodTags(any(), anyList(), anyInt())).willReturn(List.of());

        PickResult result = pickService.recommend(memberId, IMAGE_KEY, List.of(SEOUL));

        assertThat(result.spots()).isEmpty();
        assertThat(result.fallbackSpots()).isEmpty();
        assertThat(result.pickId()).isEqualTo(pickId);
    }

    @Test
    @DisplayName("무드 분석이 실패하면 재시도 가능한 오류로 바꿔 던진다")
    void recommend_wraps_analysis_failure() {
        given(imageStorageClient.issueReadUrl(IMAGE_KEY)).willReturn("https://read");
        given(moodAnalysisClient.analyze(anyString())).willThrow(new IllegalStateException("LLM 호출 실패"));

        assertThatThrownBy(() -> pickService.recommend(memberId, IMAGE_KEY, List.of(SEOUL)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PICK_ANALYSIS_FAILED);
    }

    @Test
    @DisplayName("업로드 URL을 발급할 수 없으면 그 오류를 그대로 전달한다")
    void recommend_propagates_storage_failure() {
        given(imageStorageClient.issueReadUrl(IMAGE_KEY))
                .willThrow(new BusinessException(ErrorCode.IMAGE_UPLOAD_UNAVAILABLE));

        assertThatThrownBy(() -> pickService.recommend(memberId, IMAGE_KEY, List.of(SEOUL)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_UPLOAD_UNAVAILABLE);
    }

    @Test
    @DisplayName("요청한 지역과 결과가 순위·유사도와 함께 저장된다")
    void recommend_persists_request_areas_and_results() {
        givenPickRequestSaved();
        givenAnalyzed(MoodVectorFixture.serene());
        given(pickCandidateReader.readByAreas(any(), any(PickAreas.class), anyInt()))
                .willReturn(List.of(
                        candidate(1L, partiallySerene()),
                        candidate(2L, MoodVectorFixture.serene())));

        pickService.recommend(memberId, IMAGE_KEY, List.of(SEOUL));

        ArgumentCaptor<PickRequestArea> areaCaptor = ArgumentCaptor.forClass(PickRequestArea.class);
        verify(pickRequestAreaRepository).save(areaCaptor.capture());
        assertThat(areaCaptor.getValue().getRegion()).isEqualTo("서울");

        ArgumentCaptor<PickResultSpot> resultCaptor = ArgumentCaptor.forClass(PickResultSpot.class);
        verify(pickResultSpotRepository, org.mockito.Mockito.times(2)).save(resultCaptor.capture());
        List<PickResultSpot> saved = resultCaptor.getAllValues();
        assertThat(saved).extracting(PickResultSpot::getSpotId).containsExactly(2L, 1L);
        assertThat(saved).extracting(PickResultSpot::getRank).containsExactly(0, 1);
        assertThat(saved.getFirst().getSimilarity()).isGreaterThan(saved.get(1).getSimilarity());
        assertThat(saved).allMatch(spot -> !spot.isFallback());
    }

    @Test
    @DisplayName("상위 지역과 하위 지역을 함께 고르면 상위 하나만 저장된다")
    void recommend_persists_collapsed_areas() {
        givenPickRequestSaved();
        givenAnalyzed(MoodVectorFixture.serene());
        given(pickCandidateReader.readByAreas(any(), any(PickAreas.class), anyInt()))
                .willReturn(List.of(candidate(1L, MoodVectorFixture.serene())));

        pickService.recommend(memberId, IMAGE_KEY,
                List.of(new PickArea(PickAreaLevel.DISTRICT, "서울", "성동구", null), SEOUL));

        verify(pickRequestAreaRepository, org.mockito.Mockito.times(1)).save(any(PickRequestArea.class));
    }

    @Test
    @DisplayName("저장된 추천 결과를 순위 순서대로 다시 조회한다")
    void get_pick_returns_saved_results_in_rank_order() {
        givenPickRequestFound();
        given(pickResultSpotRepository.findByPickRequestIdOrderByRank(pickId))
                .willReturn(List.of(savedResult(2L, 0, false), savedResult(1L, 1, false)));
        given(pickCandidateReader.readBySpotIds(memberId, List.of(2L, 1L)))
                .willReturn(List.of(candidate(1L, MoodVectorFixture.serene()),
                        candidate(2L, MoodVectorFixture.serene())));

        PickResult result = pickService.getPick(memberId, pickId);

        assertThat(result.pickId()).isEqualTo(pickId);
        assertThat(result.spots()).extracting(PickResultItem::spotId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("재조회는 사진을 다시 분석하지 않는다")
    void get_pick_does_not_reanalyze_image() {
        givenPickRequestFound();
        given(pickResultSpotRepository.findByPickRequestIdOrderByRank(pickId))
                .willReturn(List.of(savedResult(1L, 0, false)));
        given(pickCandidateReader.readBySpotIds(memberId, List.of(1L)))
                .willReturn(List.of(candidate(1L, MoodVectorFixture.serene())));

        pickService.getPick(memberId, pickId);

        verify(moodAnalysisClient, never()).analyze(anyString());
        verify(pickResultSpotRepository, never()).save(any(PickResultSpot.class));
    }

    @Test
    @DisplayName("대체 추천은 별도 목록으로 나뉘어 조회된다")
    void get_pick_separates_fallback_spots() {
        givenPickRequestFound();
        given(pickResultSpotRepository.findByPickRequestIdOrderByRank(pickId))
                .willReturn(List.of(savedResult(1L, 0, false), savedResult(2L, 0, true)));
        given(pickCandidateReader.readBySpotIds(memberId, List.of(1L, 2L)))
                .willReturn(List.of(candidate(1L, MoodVectorFixture.serene()),
                        candidate(2L, MoodVectorFixture.serene())));

        PickResult result = pickService.getPick(memberId, pickId);

        assertThat(result.spots()).extracting(PickResultItem::spotId).containsExactly(1L);
        assertThat(result.fallbackSpots()).extracting(PickResultItem::spotId).containsExactly(2L);
    }

    @Test
    @DisplayName("저장 이후 비활성으로 내려간 스팟은 결과에서 빠진다")
    void get_pick_drops_spots_no_longer_available() {
        givenPickRequestFound();
        given(pickResultSpotRepository.findByPickRequestIdOrderByRank(pickId))
                .willReturn(List.of(savedResult(1L, 0, false), savedResult(2L, 1, false)));
        given(pickCandidateReader.readBySpotIds(memberId, List.of(1L, 2L)))
                .willReturn(List.of(candidate(1L, MoodVectorFixture.serene())));

        PickResult result = pickService.getPick(memberId, pickId);

        assertThat(result.spots()).extracting(PickResultItem::spotId).containsExactly(1L);
    }

    @Test
    @DisplayName("존재하지 않는 추천 결과는 조회할 수 없다")
    void get_pick_with_unknown_id_throws() {
        given(pickRequestRepository.findById(pickId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pickService.getPick(memberId, pickId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PICK_NOT_FOUND);
    }

    @Test
    @DisplayName("타인의 추천 결과는 조회할 수 없다")
    void get_pick_of_other_member_throws() {
        PickRequest other = PickRequest.create(UUID.randomUUID(), IMAGE_KEY);
        ReflectionTestUtils.setField(other, "id", pickId);
        given(pickRequestRepository.findById(pickId)).willReturn(Optional.of(other));

        assertThatThrownBy(() -> pickService.getPick(memberId, pickId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PICK_FORBIDDEN);
    }

    private void givenPickRequestFound() {
        PickRequest found = PickRequest.create(memberId, IMAGE_KEY);
        ReflectionTestUtils.setField(found, "id", pickId);
        given(pickRequestRepository.findById(pickId)).willReturn(Optional.of(found));
    }

    private PickResultSpot savedResult(long spotId, int rank, boolean fallback) {
        return PickResultSpot.of(pickId, spotId, rank, 0.9, fallback);
    }

    private void givenAnalyzed(MoodVector vector) {
        given(imageStorageClient.issueReadUrl(IMAGE_KEY)).willReturn("https://read");
        given(moodAnalysisClient.analyze("https://read")).willReturn(vector);
    }

    private void givenPickRequestSaved() {
        PickRequest saved = PickRequest.create(memberId, IMAGE_KEY);
        ReflectionTestUtils.setField(saved, "id", pickId);
        given(pickRequestRepository.save(any(PickRequest.class))).willReturn(saved);
    }

    /**
     * 다섯 축은 업로드 사진과 같고 시대만 다른 벡터. 완전 일치와 완전 불일치 사이의 순위를 만든다.
     */
    private MoodVector partiallySerene() {
        return MoodVectorFixture.focused(Atmosphere.SERENE, Color.COOL, Lighting.DAYLIGHT,
                Space.NATURE, Structure.OPEN, Era.RETRO);
    }

    private PickCandidate candidate(long spotId, MoodVector vector) {
        return candidate(spotId, vector, List.of(MoodTag.NATURE));
    }

    private PickCandidate candidate(long spotId, MoodVector vector, List<MoodTag> tags) {
        return new PickCandidate(spotId, "스팟" + spotId, "https://img/" + spotId, "서울", "성동구", "성수동",
                "서울 성동구 성수동", "설명", 37.5, 127.0, tags, vector, false);
    }
}
