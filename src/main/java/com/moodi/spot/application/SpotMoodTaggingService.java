package com.moodi.spot.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotMoodTaggingService {

    private final SpotRepository spotRepository;
    private final SpotMoodTagger spotMoodTagger;
    private final MoodAnalysisClient moodAnalysisClient;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public TaggingResult tagAll(int limit) {
        int effectiveLimit = limit > 0 ? limit : Integer.MAX_VALUE;

        // 1. stale PROCESSING 복구
        int recovered = recoverStaleProcessing();
        if (recovered > 0) {
            log.info("stale PROCESSING {}건 복구 완료", recovered);
        }

        // 2. 태깅 대상 조회 (PENDING + 재시도 시각이 지난 RETRY_WAIT)
        LocalDateTime now = LocalDateTime.now(clock);
        List<Spot> targets = spotRepository.findTaggingTargets(now, effectiveLimit);

        if (targets.isEmpty()) {
            log.info("태깅 대상 없음");
            return new TaggingResult(0, 0, 0, 0, 0, List.of());
        }

        log.info("태깅 대상 스팟 {}건 조회 (limit={})", targets.size(), limit);
        moodAnalysisClient.resetCounters();
        long startNanos = System.nanoTime();

        AtomicInteger tagged = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger rateLimited = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> llmLatencies = new ConcurrentLinkedQueue<>();

        int chunkSize = 50;
        for (int i = 0; i < targets.size(); i += chunkSize) {
            List<Spot> chunk = targets.subList(i, Math.min(i + chunkSize, targets.size()));

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> futures = new ArrayList<>();

                for (Spot spot : chunk) {
                    futures.add(executor.submit(() -> {
                        try {
                            long latencyMs = spotMoodTagger.tagSpot(spot);
                            if (latencyMs >= 0) {
                                llmLatencies.add(latencyMs);
                                int count = tagged.incrementAndGet();
                                if (count % 50 == 0 || count == targets.size()) {
                                    log.info("태깅 진행 {}/{}", count, targets.size());
                                }
                            }
                        } catch (RateLimitException e) {
                            rateLimited.incrementAndGet();
                            failed.incrementAndGet();
                            log.warn("태깅 실패 (429 Rate Limit) spotId={}", spot.getId());
                        } catch (Exception e) {
                            failed.incrementAndGet();
                            log.warn("태깅 실패 spotId={}: {}", spot.getId(), e.getMessage());
                        }
                    }));
                }

                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        log.error("태깅 작업 대기 중 예외: {}", e.getMessage());
                    }
                }
            }
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        int retryCount = moodAnalysisClient.getRetryCount();
        int rateLimitCount = rateLimited.get();
        List<Long> latencyList = new ArrayList<>(llmLatencies);

        logMetrics(tagged.get(), failed.get(), retryCount, rateLimitCount,
                elapsedMs, targets.size(), latencyList);

        return new TaggingResult(
                tagged.get(), failed.get(),
                retryCount, rateLimitCount,
                elapsedMs, latencyList
        );
    }

    public int recoverStaleProcessing() {
        Integer recovered = transactionTemplate.execute(status -> {
            LocalDateTime threshold = LocalDateTime.now(clock).minusMinutes(20);
            List<Spot> staleSpots = spotRepository.findStaleProcessing(threshold);

            int count = 0;
            LocalDateTime now = LocalDateTime.now(clock);
            for (Spot spot : staleSpots) {
                if (spot.recoverStaleProcessing(now)) {
                    spotRepository.save(spot);
                    count++;
                    log.warn("stale PROCESSING 복구 spotId={}, attemptCount={}",
                            spot.getId(), spot.getMoodTaggingAttemptCount());
                }
            }
            return count;
        });
        return recovered != null ? recovered : 0;
    }

    private void logMetrics(int tagged, int failed, int retryCount,
                            int rateLimitCount, long elapsedMs, int totalTargets,
                            List<Long> latencies) {
        double elapsedSec = elapsedMs / 1000.0;
        double throughput = elapsedSec > 0 ? tagged / elapsedSec : 0;

        log.info("═══════════════════════════════════════════════════════════");
        log.info("태깅 완료 — 결과 요약");
        log.info("───────────────────────────────────────────────────────────");
        log.info("  대상: {}건 | 성공: {}건 | 실패: {}건",
                totalTargets, tagged, failed);
        log.info("  전체 처리 시간: {}초", String.format("%.1f", elapsedSec));
        log.info("  throughput: {} spots/sec", String.format("%.2f", throughput));
        log.info("  retry: {}건 | 429: {}건", retryCount, rateLimitCount);

        if (!latencies.isEmpty()) {
            Collections.sort(latencies);
            long sum = latencies.stream().mapToLong(Long::longValue).sum();
            long avg = sum / latencies.size();
            int p95Index = (int) Math.ceil(latencies.size() * 0.95) - 1;
            long p95 = latencies.get(Math.max(0, p95Index));

            log.info("  LLM latency — avg: {}ms | p95: {}ms | min: {}ms | max: {}ms",
                    avg, p95, latencies.getFirst(), latencies.getLast());
        }
        log.info("═══════════════════════════════════════════════════════════");
    }

    public record TaggingResult(
            int tagged, int failed,
            int retryCount, int rateLimitCount,
            long elapsedMs, List<Long> llmLatencies
    ) {}
}
