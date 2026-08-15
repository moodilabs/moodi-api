package com.moodi.spot.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotDescriptionRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SpotDescriptionBatchService {

    private final SpotDescriptionRepository descriptionRepository;
    private final SpotRepository spotRepository;
    private final SpotTranslationRepository translationRepository;
    private final SpotDescriptionClient descriptionClient;
    private final SpotDescriptionWriter descriptionWriter;
    private final Semaphore llmSemaphore;

    public SpotDescriptionBatchService(
            SpotDescriptionRepository descriptionRepository,
            SpotRepository spotRepository,
            SpotTranslationRepository translationRepository,
            SpotDescriptionClient descriptionClient,
            SpotDescriptionWriter descriptionWriter,
            @Value("${spot-description.concurrency:3}") int concurrency
    ) {
        if (concurrency < 1) {
            throw new IllegalArgumentException("spot-description.concurrency must be >= 1, but was " + concurrency);
        }
        this.descriptionRepository = descriptionRepository;
        this.spotRepository = spotRepository;
        this.translationRepository = translationRepository;
        this.descriptionClient = descriptionClient;
        this.descriptionWriter = descriptionWriter;
        this.llmSemaphore = new Semaphore(concurrency);
    }

    public BatchResult generateAll(String locale, int limit) {
        List<Long> targetIds = descriptionRepository.findSpotIdsWithoutDescription(locale);
        if (limit > 0) {
            targetIds = targetIds.subList(0, Math.min(limit, targetIds.size()));
        }
        log.info("설명 생성 대상 {}건 조회 (locale={}, limit={})", targetIds.size(), locale, limit);

        if (targetIds.isEmpty()) {
            return new BatchResult(0, 0, 0, List.of());
        }

        List<Spot> spots = spotRepository.findByIdIn(targetIds);

        long startNanos = System.nanoTime();
        AtomicInteger generated = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> llmLatencies = new ConcurrentLinkedQueue<>();

        int chunkSize = 50;
        for (int i = 0; i < spots.size(); i += chunkSize) {
            List<Spot> chunk = spots.subList(i, Math.min(i + chunkSize, spots.size()));

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<?>> futures = new ArrayList<>();

                for (Spot spot : chunk) {
                    futures.add(executor.submit(() -> {
                        try {
                            long latencyMs = generateOne(spot, locale);
                            llmLatencies.add(latencyMs);
                            int count = generated.incrementAndGet();
                            if (count % 50 == 0 || count == spots.size()) {
                                log.info("설명 생성 진행 {}/{}", count, spots.size());
                            }
                        } catch (RateLimitException e) {
                            failed.incrementAndGet();
                            log.warn("설명 생성 실패 (429 Rate Limit) spotId={}", spot.getId());
                        } catch (Exception e) {
                            failed.incrementAndGet();
                            log.warn("설명 생성 실패 spotId={}: {}", spot.getId(), e.getMessage());
                        }
                    }));
                }

                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        log.error("설명 생성 작업 대기 중 예외: {}", e.getMessage());
                    }
                }
            }
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        List<Long> latencyList = new ArrayList<>(llmLatencies);

        logMetrics(generated.get(), failed.get(), elapsedMs, spots.size(), latencyList);

        return new BatchResult(generated.get(), failed.get(), elapsedMs, latencyList);
    }

    private long generateOne(Spot spot, String locale) {
        SpotTranslation translation = translationRepository
                .findBySpotIdAndLocale(spot.getId(), "ko-KR")
                .orElse(null);

        if (translation == null || translation.getOverview() == null || translation.getOverview().isBlank()) {
            throw new IllegalStateException("KO overview 없음 — 다음 배치에서 재시도 (spotId=" + spot.getId() + ")");
        }

        String title = translation.getTitle();
        String overview = translation.getOverview();

        llmSemaphore.acquireUninterruptibly();
        long startNanos = System.nanoTime();
        try {
            String content = descriptionClient.generate(
                    title,
                    spot.getContentType().name(),
                    spot.getArea(),
                    "",
                    overview,
                    locale
            );
            descriptionWriter.saveIfAbsent(spot.getId(), locale, content);
        } finally {
            llmSemaphore.release();
        }

        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private void logMetrics(int generated, int failed, long elapsedMs,
                            int totalTargets, List<Long> latencies) {
        double elapsedSec = elapsedMs / 1000.0;
        double throughput = elapsedSec > 0 ? generated / elapsedSec : 0;

        log.info("═══════════════════════════════════════════════════════════");
        log.info("설명 생성 완료 — 결과 요약");
        log.info("───────────────────────────────────────────────────────────");
        log.info("  대상: {}건 | 성공: {}건 | 실패: {}건", totalTargets, generated, failed);
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
            int generated, int failed,
            long elapsedMs, List<Long> llmLatencies
    ) {}
}
