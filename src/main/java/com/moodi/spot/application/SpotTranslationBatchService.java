package com.moodi.spot.application;

import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class SpotTranslationBatchService {

    private static final String SOURCE_LOCALE = "ko-KR";

    private final SpotTranslationRepository translationRepository;
    private final SpotTranslationClient translationClient;
    private final Semaphore llmSemaphore;

    public SpotTranslationBatchService(
            SpotTranslationRepository translationRepository,
            SpotTranslationClient translationClient,
            @Value("${spot-translation.concurrency:3}") int concurrency
    ) {
        if (concurrency < 1) {
            throw new IllegalArgumentException("spot-translation.concurrency must be >= 1, but was " + concurrency);
        }
        this.translationRepository = translationRepository;
        this.translationClient = translationClient;
        this.llmSemaphore = new Semaphore(concurrency);
    }

    public BatchResult translateAll(String targetLocale, int limit) {
        List<Long> allIds = translationRepository.findSpotIdsWithoutTranslation(targetLocale);
        List<Long> targetIds = limit > 0
                ? allIds.subList(0, Math.min(limit, allIds.size()))
                : allIds;
        log.info("번역 대상 {}건 조회 (targetLocale={}, limit={})", targetIds.size(), targetLocale, limit);

        if (targetIds.isEmpty()) {
            return new BatchResult(0, 0, 0, List.of());
        }

        long startNanos = System.nanoTime();
        AtomicInteger translated = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> llmLatencies = new ConcurrentLinkedQueue<>();

        int chunkSize = 50;
        for (int i = 0; i < targetIds.size(); i += chunkSize) {
            List<Long> chunk = targetIds.subList(i, Math.min(i + chunkSize, targetIds.size()));

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> futures = new ArrayList<>();

                for (Long spotId : chunk) {
                    futures.add(executor.submit(() -> {
                        try {
                            long latencyMs = translateOne(spotId, targetLocale);
                            llmLatencies.add(latencyMs);
                            int count = translated.incrementAndGet();
                            if (count % 50 == 0 || count == targetIds.size()) {
                                log.info("번역 진행 {}/{}", count, targetIds.size());
                            }
                        } catch (RateLimitException e) {
                            failed.incrementAndGet();
                            log.warn("번역 실패 (429 Rate Limit) spotId={}", spotId);
                        } catch (Exception e) {
                            failed.incrementAndGet();
                            log.warn("번역 실패 spotId={}: {}", spotId, e.getMessage());
                        }
                    }));
                }

                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        log.error("번역 작업 대기 중 예외: {}", e.getMessage());
                    }
                }
            }
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        List<Long> latencyList = new ArrayList<>(llmLatencies);

        logMetrics(translated.get(), failed.get(), elapsedMs, targetIds.size(), latencyList);

        return new BatchResult(translated.get(), failed.get(), elapsedMs, latencyList);
    }

    private long translateOne(Long spotId, String targetLocale) {
        SpotTranslation source = translationRepository
                .findBySpotIdAndLocale(spotId, SOURCE_LOCALE)
                .orElseThrow(() -> new IllegalStateException(
                        "KO 번역 원본 없음 — 번역 불가 (spotId=" + spotId + ")"));

        llmSemaphore.acquireUninterruptibly();
        long startNanos = System.nanoTime();
        try {
            SpotTranslationClient.TranslatedSpot result = translationClient.translate(
                    source.getTitle(),
                    source.getAddr1(),
                    source.getAddr2()
            );

            SpotTranslation enTranslation = SpotTranslation.create(
                    spotId, targetLocale,
                    result.title(), null,
                    result.addr1(), result.addr2()
            );
            translationRepository.save(enTranslation);
        } finally {
            llmSemaphore.release();
        }

        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private void logMetrics(int translated, int failed, long elapsedMs,
                            int totalTargets, List<Long> latencies) {
        double elapsedSec = elapsedMs / 1000.0;
        double throughput = elapsedSec > 0 ? translated / elapsedSec : 0;

        log.info("═══════════════════════════════════════════════════════════");
        log.info("번역 배치 완료 — 결과 요약");
        log.info("───────────────────────────────────────────────────────────");
        log.info("  대상: {}건 | 성공: {}건 | 실패: {}건", totalTargets, translated, failed);
        log.info("  전체 처리 시간: {}초", String.format("%.1f", elapsedSec));
        log.info("  throughput: {} spots/sec", String.format("%.2f", throughput));

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

    public record BatchResult(
            int translated, int failed,
            long elapsedMs, List<Long> llmLatencies
    ) {
    }
}
