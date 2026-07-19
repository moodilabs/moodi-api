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

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@ConditionalOnProperty(name = "spot-import.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SpotDataImportRunner implements ApplicationRunner {

    private final SpotCsvReader spotCsvReader;
    private final SpotDataLoader spotDataLoader;
    private final ApplicationContext applicationContext;

    @Value("${spot-import.path}")
    private String csvPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path path = Path.of(csvPath);
        log.info("스팟 데이터 적재 시작: {}", path);

        int[] exitCode = {0};
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var readResult = spotCsvReader.read(reader);
            if (readResult.failedRows() > 0) {
                log.warn("CSV 파싱 실패 {}건 — 정상 행만 적재합니다.", readResult.failedRows());
            }
            var loadResult = spotDataLoader.load(readResult.rows());
            log.info("스팟 데이터 적재 결과: 저장 {}건, 스킵 {}건, 적재실패 {}건, 파싱실패 {}건",
                    loadResult.saved(), loadResult.skipped(), loadResult.failed(), readResult.failedRows());

            if (loadResult.failed() > 0 || readResult.failedRows() > 0) {
                exitCode[0] = 1;
            }
        } catch (Exception e) {
            log.error("스팟 데이터 적재 중 예외 발생: {}", e.getMessage(), e);
            exitCode[0] = 1;
        } finally {
            SpringApplication.exit(applicationContext, () -> exitCode[0]);
        }
    }
}
