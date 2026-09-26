package com.moodi.spot.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.mood.MoodTagRuleEngine;
import com.moodi.shared.mood.MoodVector;
import com.moodi.spot.application.MoodAnalysisClient.MoodAnalysisResult;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotMood;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class SpotMoodTagger {

    private final SpotRepository spotRepository;
    private final SpotImageRepository spotImageRepository;
    private final SpotTranslationRepository spotTranslationRepository;
    private final SpotMoodRepository spotMoodRepository;
    private final MoodAnalysisClient moodAnalysisClient;
    private final MoodTagRuleEngine moodTagRuleEngine;
    private final TransactionTemplate transactionTemplate;
    private final Semaphore llmSemaphore;
    private final Clock clock;

    public SpotMoodTagger(
            SpotRepository spotRepository,
            SpotImageRepository spotImageRepository,
            SpotTranslationRepository spotTranslationRepository,
            SpotMoodRepository spotMoodRepository,
            MoodAnalysisClient moodAnalysisClient,
            MoodTagRuleEngine moodTagRuleEngine,
            PlatformTransactionManager transactionManager,
            Clock clock,
            @Value("${spot-tagging.concurrency:1}") int concurrency
    ) {
        if (concurrency < 1) {
            throw new IllegalArgumentException("spot-tagging.concurrency must be >= 1, but was " + concurrency);
        }
        this.spotRepository = spotRepository;
        this.spotImageRepository = spotImageRepository;
        this.spotTranslationRepository = spotTranslationRepository;
        this.spotMoodRepository = spotMoodRepository;
        this.moodAnalysisClient = moodAnalysisClient;
        this.moodTagRuleEngine = moodTagRuleEngine;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.llmSemaphore = new Semaphore(concurrency);
    }

    /** 선점 실패 (이미 다른 작업자가 처리 중) — 정상 skip. */
    public static final long CLAIM_SKIPPED = -1;
    /** 태깅 오류 처리 완료 (RETRY_WAIT 또는 FAILED 전이됨) — 실패 카운트 대상. */
    public static final long HANDLED_ERROR = -2;

    /**
     * 1. DB에서 PROCESSING으로 선점 (짧은 트랜잭션, 커밋)
     * 2. LLM 호출 (트랜잭션 밖)
     * 3. 결과 저장 + 상태 전이 (짧은 트랜잭션)
     *
     * @return LLM 호출 소요시간 (ms), 선점 실패 시 CLAIM_SKIPPED, 오류 처리 시 HANDLED_ERROR
     */
    public long tagSpot(Spot spot) {
        // 1. PROCESSING 선점
        boolean claimed = claimForProcessing(spot);
        if (!claimed) {
            log.debug("선점 실패 spotId={} (이미 다른 작업자가 처리 중)", spot.getId());
            return CLAIM_SKIPPED;
        }

        // 2. DB 조회 — 선점 후이므로 실패 시 상태 전이 필요
        List<String> imageUrls;
        String overview;
        try {
            imageUrls = spotImageRepository.findBySpotId(spot.getId()).stream()
                    .map(SpotImage::getImageUrl)
                    .toList();
            overview = spotTranslationRepository.findBySpotIdAndLocale(spot.getId(), "ko-KR")
                    .map(SpotTranslation::getOverview)
                    .orElse("");
        } catch (Exception e) {
            return handleErrorAndReturn(spot, "DB 조회 실패: " + e.getMessage(), false);
        }

        // 3. LLM 호출 (트랜잭션 밖, Semaphore로 동시 요청 수 제한)
        MoodAnalysisResult result;
        llmSemaphore.acquireUninterruptibly();
        long llmStartNanos = System.nanoTime();
        try {
            result = moodAnalysisClient.analyze(imageUrls, overview);
        } catch (RateLimitException e) {
            tryTransientError(spot, "429 Rate Limit: " + e.getMessage());
            throw e;
        } catch (IllegalStateException e) {
            return handleErrorAndReturn(spot, "LLM 응답 오류: " + e.getMessage(), true);
        } catch (Exception e) {
            return handleErrorAndReturn(spot, e.getClass().getSimpleName() + ": " + e.getMessage(), false);
        } finally {
            llmSemaphore.release();
        }
        long llmLatencyMs = (System.nanoTime() - llmStartNanos) / 1_000_000;

        MoodVector vector = result.moodVector();
        List<MoodTag> tags = moodTagRuleEngine.deriveTags(vector, result.seasonalScore());

        // 4. DB 저장 + 상태 전이 (짧은 트랜잭션)
        transactionTemplate.executeWithoutResult(status -> {
            Spot freshSpot = spotRepository.findById(spot.getId()).orElseThrow();

            SpotMood spotMood = SpotMood.create(freshSpot.getId(), vector, tags, result.confidence());
            spotMoodRepository.save(spotMood);

            freshSpot.completeTagging(LocalDateTime.now(clock));
            freshSpot.publish();
            spotRepository.save(freshSpot);
        });

        return llmLatencyMs;
    }

    private boolean claimForProcessing(Spot spot) {
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                Spot freshSpot = spotRepository.findById(spot.getId()).orElseThrow();
                freshSpot.startTagging(LocalDateTime.now(clock));
                spotRepository.save(freshSpot);
                return true;
            }));
        } catch (IllegalStateException e) {
            log.debug("선점 실패 spotId={}: {}", spot.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * 오류 상태 저장 후 적절한 반환값을 결정한다.
     * 저장 성공 시 HANDLED_ERROR, 저장 실패 시 RuntimeException을 던져 tagAll이 예외로 집계하도록 한다.
     *
     * @param dataError true면 FAILED (데이터 문제), false면 RETRY_WAIT (일시 오류)
     */
    private long handleErrorAndReturn(Spot spot, String error, boolean dataError) {
        if (saveErrorState(spot, error, dataError)) {
            return HANDLED_ERROR;
        }
        throw new RuntimeException("태깅 오류 상태 저장 실패 spotId=" + spot.getId() + ": " + error);
    }

    /** RateLimitException 전용 — 항상 예외를 다시 던지므로 저장 실패 여부만 로그로 남긴다. */
    private void tryTransientError(Spot spot, String error) {
        saveErrorState(spot, error, false);
    }

    /** @return 상태 저장 성공 여부 */
    private boolean saveErrorState(Spot spot, String error, boolean dataError) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Spot freshSpot = spotRepository.findById(spot.getId()).orElseThrow();
                if (dataError) {
                    freshSpot.markFailed(error, LocalDateTime.now(clock));
                } else {
                    freshSpot.markRetryWait(error, LocalDateTime.now(clock));
                }
                spotRepository.save(freshSpot);
            });
            return true;
        } catch (Exception e) {
            log.error("오류 상태 저장 실패 spotId={}: {}", spot.getId(), e.getMessage());
            return false;
        }
    }
}
