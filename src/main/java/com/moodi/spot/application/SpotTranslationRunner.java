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
@ConditionalOnProperty(name = "spot-translation.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SpotTranslationRunner implements ApplicationRunner {

    private final SpotTranslationBatchService batchService;
    private final ApplicationContext applicationContext;

    @Value("${spot-translation.target-locale:en-US}")
    private String targetLocale;

    @Value("${spot-translation.limit:0}")
    private int limit;

    @Value("${spot-translation.concurrency:3}")
    private int concurrency;

    @Override
    public void run(ApplicationArguments args) {
        log.info("스팟 번역 배치 시작 (targetLocale={}, limit={}, concurrency={})",
                targetLocale, limit, concurrency);

        int[] exitCode = {0};
        try {
            SpotTranslationBatchService.BatchResult result =
                    batchService.translateAll(targetLocale, limit);
            log.info("스팟 번역 배치 결과: 성공 {}건, 실패 {}건 (소요 {}ms)",
                    result.translated(), result.failed(), result.elapsedMs());

            if (result.failed() > 0) {
                exitCode[0] = 1;
            }
        } catch (Exception e) {
            log.error("스팟 번역 배치 중 예외 발생: {}", e.getMessage(), e);
            exitCode[0] = 1;
        } finally {
            int code = SpringApplication.exit(applicationContext, () -> exitCode[0]);
            System.exit(code);
        }
    }
}
