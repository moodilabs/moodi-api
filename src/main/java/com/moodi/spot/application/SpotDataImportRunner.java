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
@ConditionalOnProperty(name = "spot-import.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SpotDataImportRunner implements ApplicationRunner {

    private final SpotImportService spotImportService;
    private final ApplicationContext applicationContext;

    @Value("${spot-import.path}")
    private String csvPath;

    @Override
    public void run(ApplicationArguments args) {
        SpotImportService.ImportResult result = spotImportService.run(csvPath);
        int exitCode = result.success() ? 0 : 1;
        System.exit(SpringApplication.exit(applicationContext, () -> exitCode));
    }
}
