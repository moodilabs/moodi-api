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
@ConditionalOnProperty(name = "spot-description.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SpotDescriptionRunner implements ApplicationRunner {

    private final SpotDescriptionBatchService batchService;
    private final ApplicationContext applicationContext;

    @Value("${spot-description.locale:en-US}")
    private String locale;

    @Value("${spot-description.limit:0}")
    private int limit;

    @Value("${spot-description.concurrency:3}")
    private int concurrency;

    @Override
    public void run(ApplicationArguments args) {
        log.info("스팟 설명 생성 시작 (locale={}, limit={}, concurrency={})", locale, limit, concurrency);

        int[] exitCode = {0};
        try {
            SpotDescriptionBatchService.BatchResult result = batchService.generateAll(locale, limit);
            log.info("스팟 설명 생성 결과: 성공 {}건, 실패 {}건 (소요 {}ms)",
                    result.generated(), result.failed(), result.elapsedMs());

            if (result.failed() > 0) {
                exitCode[0] = 1;
            }
        } catch (Exception e) {
            log.error("스팟 설명 생성 중 예외 발생: {}", e.getMessage(), e);
            exitCode[0] = 1;
        } finally {
            int code = SpringApplication.exit(applicationContext, () -> exitCode[0]);
            System.exit(code);
        }
    }
}
