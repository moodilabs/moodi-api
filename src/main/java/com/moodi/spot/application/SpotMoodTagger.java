package com.moodi.spot.application;

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

    public SpotMoodTagger(
            SpotRepository spotRepository,
            SpotImageRepository spotImageRepository,
            SpotTranslationRepository spotTranslationRepository,
            SpotMoodRepository spotMoodRepository,
            MoodAnalysisClient moodAnalysisClient,
            MoodTagRuleEngine moodTagRuleEngine,
            PlatformTransactionManager transactionManager,
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
        this.llmSemaphore = new Semaphore(concurrency);
    }

    /**
     * @return LLM 호출 소요시간 (ms)
     */
    public long tagSpot(Spot spot) {
        // 1. DB 조회 (트랜잭션 없이 auto-commit)
        List<String> imageUrls = spotImageRepository.findBySpotId(spot.getId()).stream()
                .map(SpotImage::getImageUrl)
                .toList();

        String overview = spotTranslationRepository.findBySpotIdAndLocale(spot.getId(), "ko")
                .map(SpotTranslation::getOverview)
                .orElse("");

        // 2. LLM 호출 (트랜잭션 밖, Semaphore로 동시 요청 수 제한)
        MoodAnalysisResult result;
        llmSemaphore.acquireUninterruptibly();
        long llmStartNanos = System.nanoTime();
        try {
            result = moodAnalysisClient.analyze(imageUrls, overview);
        } finally {
            llmSemaphore.release();
        }
        long llmLatencyMs = (System.nanoTime() - llmStartNanos) / 1_000_000;

        MoodVector vector = result.moodVector();
        List<MoodTag> tags = moodTagRuleEngine.deriveTags(vector, result.seasonalScore());

        // 3. DB 저장 (짧은 트랜잭션)
        transactionTemplate.executeWithoutResult(status -> {
            SpotMood spotMood = SpotMood.create(spot.getId(), vector, tags, result.confidence());
            spotMoodRepository.save(spotMood);

            spot.publish();
            spotRepository.save(spot);
        });

        return llmLatencyMs;
    }
}
