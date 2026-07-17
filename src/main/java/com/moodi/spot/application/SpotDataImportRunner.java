package com.moodi.spot.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Value("${spot-import.path}")
    private String csvPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path path = Path.of(csvPath);
        log.info("스팟 데이터 적재 시작: {}", path);

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var rows = spotCsvReader.read(reader);
            var result = spotDataLoader.load(rows);
            log.info("스팟 데이터 적재 결과: 저장 {}건, 스킵 {}건, 실패 {}건",
                    result.saved(), result.skipped(), result.failed());
        }
    }
}
