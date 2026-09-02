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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * 저장해 둔 추천 결과를 다시 조회한다 (DSC-05 재조회).
     *
     * <p>사진을 다시 분석하지 않는다. 같은 사진이라도 분석 결과가 매번 미세하게 달라질 수 있어
     * 재조회 때마다 순서가 바뀌면 "모달 종료 시 기존 결과 유지" 요구를 만족하지 못한다.
     * 그래서 순서는 저장된 순위를 그대로 쓰고, 스팟 내용만 최신으로 채운다.
     */
    public PickResult getPick(UUID memberId, UUID pickId) {
        PickRequest pickRequest = pickRequestRepository.findById(pickId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PICK_NOT_FOUND));
        if (!pickRequest.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.PICK_FORBIDDEN);
        }

        List<PickResultSpot> saved = pickResultSpotRepository.findByPickRequestIdOrderByRank(pickId);
        Map<Long, PickCandidate> candidates = readCandidates(memberId, saved);

        return new PickResult(
                pickId,
                hydrate(saved, candidates, false),
                hydrate(saved, candidates, true)
        );
    }

    private Map<Long, PickCandidate> readCandidates(UUID memberId, List<PickResultSpot> saved) {
        List<Long> spotIds = saved.stream().map(PickResultSpot::getSpotId).distinct().toList();
        Map<Long, PickCandidate> bySpotId = new LinkedHashMap<>();
        for (PickCandidate candidate : pickCandidateReader.readBySpotIds(memberId, spotIds)) {
            bySpotId.put(candidate.spotId(), candidate);
        }
        return bySpotId;
    }

    /**
     * 저장 이후 비활성으로 내려간 스팟은 조회에서 빠지므로 결과가 저장 시점보다 적을 수 있다.
     * 빈 자리를 다른 스팟으로 메우지는 않는다. 재조회는 그때 본 목록을 되살리는 것이지 새로 추천하는 게 아니다.
     */
    private List<PickResultItem> hydrate(List<PickResultSpot> saved, Map<Long, PickCandidate> candidates,
                                         boolean fallback) {
        return saved.stream()
                .filter(result -> result.isFallback() == fallback)
                .map(result -> candidates.get(result.getSpotId()))
                .filter(candidate -> candidate != null)
                .map(PickResultItem::from)
                .toList();
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
