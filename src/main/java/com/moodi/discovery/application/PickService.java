package com.moodi.discovery.application;

import com.moodi.discovery.domain.MoodSimilarity;
import com.moodi.discovery.domain.PickArea;
import com.moodi.discovery.domain.PickAreas;
import com.moodi.discovery.domain.PickRequest;
import com.moodi.discovery.domain.PickRequestArea;
import com.moodi.discovery.domain.PickRequestAreaRepository;
import com.moodi.discovery.domain.PickRequestRepository;
import com.moodi.discovery.domain.PickResultSpot;
import com.moodi.discovery.domain.PickResultSpotRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.mood.MoodTagRuleEngine;
import com.moodi.shared.mood.MoodVector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 사진 1장과 지역으로 스팟을 추천한다 (DSC-04 → DSC-05).
 *
 * <p>후보 선별은 SQL에서, 순위 매기기는 여기에서 한다. 무드 벡터가 JSONB 맵이라
 * SQL로 코사인 유사도를 계산하려면 6개 축을 전부 펼쳐야 해서 쿼리가 읽기 어려워지고 인덱스도 못 탄다.
 * 후보를 먼저 좁힌 뒤라 자바에서 정렬해도 감당된다.
 */
@Service
@Transactional(readOnly = true)
public class PickService {

    private static final int RESULT_LIMIT = 5;

    private final MoodAnalysisClient moodAnalysisClient;
    private final ImageStorageClient imageStorageClient;
    private final PickCandidateReader pickCandidateReader;
    private final PickRequestRepository pickRequestRepository;
    private final PickRequestAreaRepository pickRequestAreaRepository;
    private final PickResultSpotRepository pickResultSpotRepository;
    private final MoodTagRuleEngine moodTagRuleEngine;
    private final PickProperties pickProperties;

    public PickService(MoodAnalysisClient moodAnalysisClient,
                       ImageStorageClient imageStorageClient,
                       PickCandidateReader pickCandidateReader,
                       PickRequestRepository pickRequestRepository,
                       PickRequestAreaRepository pickRequestAreaRepository,
                       PickResultSpotRepository pickResultSpotRepository,
                       MoodTagRuleEngine moodTagRuleEngine,
                       PickProperties pickProperties) {
        this.moodAnalysisClient = moodAnalysisClient;
        this.imageStorageClient = imageStorageClient;
        this.pickCandidateReader = pickCandidateReader;
        this.pickRequestRepository = pickRequestRepository;
        this.pickRequestAreaRepository = pickRequestAreaRepository;
        this.pickResultSpotRepository = pickResultSpotRepository;
        this.moodTagRuleEngine = moodTagRuleEngine;
        this.pickProperties = pickProperties;
    }

    @Transactional
    public PickResult recommend(UUID memberId, String imageKey, List<PickArea> requestedAreas) {
        PickAreas areas = PickAreas.of(requestedAreas);
        MoodVector uploaded = analyze(imageKey);

        List<Ranked> spots = rank(
                pickCandidateReader.readByAreas(memberId, areas, pickProperties.candidateLimit()), uploaded);

        // 지역 안에서 못 찾았을 때만 조건을 풀어 다시 찾는다. 매번 뽑으면 쓰이지 않을 결과를 위해
        // 조회를 한 번 더 하게 된다.
        List<Ranked> fallbackSpots = spots.isEmpty() ? rankFallback(memberId, uploaded) : List.of();

        UUID pickId = persist(memberId, imageKey, areas, spots, fallbackSpots);
        return new PickResult(pickId, toItems(spots), toItems(fallbackSpots));
    }

    private List<Ranked> rankFallback(UUID memberId, MoodVector uploaded) {
        List<MoodTag> tags = moodTagRuleEngine.deriveTags(uploaded);
        if (tags.isEmpty()) {
            return List.of();
        }
        return rank(pickCandidateReader.readByMoodTags(memberId, tags, pickProperties.candidateLimit()), uploaded);
    }

    private MoodVector analyze(String imageKey) {
        // 비공개 버킷이라 분석기가 바로 읽을 수 없다. 읽기용 서명 URL을 그때그때 발급해 넘긴다.
        String readUrl = imageStorageClient.issueReadUrl(imageKey);
        try {
            return moodAnalysisClient.analyze(readUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.PICK_ANALYSIS_FAILED);
        }
    }

    /**
     * 무드 벡터가 없는 스팟은 유사도를 잴 수 없으므로 후보에서 빠진다.
     */
    private List<Ranked> rank(List<PickCandidate> candidates, MoodVector uploaded) {
        return candidates.stream()
                .filter(candidate -> candidate.moodVector() != null)
                .map(candidate -> new Ranked(candidate, MoodSimilarity.between(uploaded, candidate.moodVector())))
                .sorted(Comparator.comparingDouble(Ranked::similarity).reversed()
                        .thenComparingLong(ranked -> ranked.candidate().spotId()))
                .limit(RESULT_LIMIT)
                .toList();
    }

    private UUID persist(UUID memberId, String imageKey, PickAreas areas,
                         List<Ranked> spots, List<Ranked> fallbackSpots) {
        PickRequest pickRequest = pickRequestRepository.save(PickRequest.create(memberId, imageKey));

        List<PickArea> values = areas.values();
        for (int i = 0; i < values.size(); i++) {
            pickRequestAreaRepository.save(PickRequestArea.of(pickRequest.getId(), values.get(i), i));
        }
        saveResults(pickRequest.getId(), spots, false);
        saveResults(pickRequest.getId(), fallbackSpots, true);

        return pickRequest.getId();
    }

    private void saveResults(UUID pickRequestId, List<Ranked> ranked, boolean fallback) {
        for (int i = 0; i < ranked.size(); i++) {
            Ranked entry = ranked.get(i);
            pickResultSpotRepository.save(PickResultSpot.of(
                    pickRequestId, entry.candidate().spotId(), i, entry.similarity(), fallback));
        }
    }

    private List<PickResultItem> toItems(List<Ranked> ranked) {
        return ranked.stream().map(entry -> PickResultItem.from(entry.candidate())).toList();
    }

    private record Ranked(PickCandidate candidate, double similarity) {
    }
}
