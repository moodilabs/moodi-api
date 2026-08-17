package com.moodi.spot.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "spot-pipeline.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SpotPipelineRunner implements ApplicationRunner {

    private final SpotImportService importService;
    private final SpotImageMigrationService imageMigrationService;
    private final SpotMoodTaggingService taggingService;
    private final ApplicationContext applicationContext;

    @Value("${spot-pipeline.path}")
    private String csvPath;

    @Override
    public void run(ApplicationArguments args) {
        log.info("===== 스팟 파이프라인 시작 =====");
        long totalStart = System.currentTimeMillis();
        boolean allSuccess = true;

        // 1단계: CSV 적재
        log.info("[1/3] CSV 적재");
        SpotImportService.ImportResult importResult = importService.run(csvPath);
        if (!importResult.success()) {
            log.warn("[1/3] CSV 적재 일부 실패 — 다음 단계 계속 진행");
            allSuccess = false;
        }

        // 2단계: 이미지 GCS 업로드
        log.info("[2/3] 이미지 GCS 마이그레이션");
        SpotImageMigrationService.MigrationResult migrationResult = imageMigrationService.run();
        if (!migrationResult.success()) {
            log.warn("[2/3] 이미지 마이그레이션 일부 실패 — 다음 단계 계속 진행");
            allSuccess = false;
        }

        // 3단계: 무드 태깅
        log.info("[3/3] 무드 태깅");
        SpotMoodTaggingService.TaggingResult taggingResult = taggingService.tagAll(0);
        if (taggingResult.failed() > 0) {
            log.warn("[3/3] 무드 태깅 일부 실패");
            allSuccess = false;
        }

        long totalElapsed = System.currentTimeMillis() - totalStart;
        log.info("===== 스팟 파이프라인 완료 (소요시간 {}ms) =====", totalElapsed);

        int exitCode = allSuccess ? 0 : 1;
        int code = SpringApplication.exit(applicationContext, () -> exitCode);
        System.exit(code);
    }
}
