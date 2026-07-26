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
@ConditionalOnProperty(name = "spot-tagging.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SpotMoodTaggingRunner implements ApplicationRunner {

    private final SpotMoodTaggingService taggingService;
    private final ApplicationContext applicationContext;

    @Value("${spot-tagging.limit:0}")
    private int limit;

    @Override
    public void run(ApplicationArguments args) {
        log.info("스팟 무드 태깅 시작 (limit={})", limit);

        int[] exitCode = {0};
        try {
            SpotMoodTaggingService.TaggingResult result = taggingService.tagAll(limit);
            log.info("스팟 무드 태깅 결과: 성공 {}건, 스킵 {}건, 실패 {}건 (invalid 응답 {}건 / 전체 요청 {}건)",
                    result.tagged(), result.skipped(), result.failed(),
                    result.invalidResponses(), result.tagged() + result.failed());

            if (result.failed() > 0) {
                exitCode[0] = 1;
            }
        } catch (Exception e) {
            log.error("스팟 무드 태깅 중 예외 발생: {}", e.getMessage(), e);
            exitCode[0] = 1;
        } finally {
            int code = SpringApplication.exit(applicationContext, () -> exitCode[0]);
            System.exit(code);
        }
    }
}
