package com.moodi.spot.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "spot-image-migration.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SpotImageMigrationRunner implements ApplicationRunner {

    private final SpotImageMigrationService migrationService;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("스팟 이미지 GCS 마이그레이션 시작");
        SpotImageMigrationService.MigrationResult result = migrationService.run();
        int exitCode = result.success() ? 0 : 1;
        SpringApplication.exit(applicationContext, () -> exitCode);
    }
}
