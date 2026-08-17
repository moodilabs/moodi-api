package com.moodi.spot.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ConditionalOnProperty(name = "spot-image-migration.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SpotImageMigrationRunner implements ApplicationRunner {

    private static final int BATCH_SIZE = 200;

    private static final String SELECT_NON_GCS_IMAGES = """
            SELECT si.id, si.image_url, s.source, s.content_id
            FROM spot_image si
            JOIN spot s ON s.id = si.spot_id
            WHERE si.image_url NOT LIKE 'https://storage.googleapis.com/%'
            ORDER BY si.id
            """;

    private static final String UPDATE_IMAGE_URL = """
            UPDATE spot_image SET image_url = :gcsUrl, updated_at = NOW()
            WHERE id = :id
            """;

    private final SpotImageUploader imageUploader;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("스팟 이미지 GCS 마이그레이션 시작");

        int[] exitCode = {0};
        try {
            List<ImageMigrationRow> rows = findNonGcsImages();
            log.info("마이그레이션 대상: {}건", rows.size());

            if (rows.isEmpty()) {
                log.info("마이그레이션 대상 이미지가 없습니다.");
                return;
            }

            MigrationResult result = migrate(rows);
            log.info("마이그레이션 완료: 성공 {}건, 실패 {}건, 전체 {}건",
                    result.success(), result.failed(), rows.size());

            if (result.failed() > 0) {
                exitCode[0] = 1;
            }
        } catch (Exception e) {
            log.error("마이그레이션 중 예외 발생: {}", e.getMessage(), e);
            exitCode[0] = 1;
        } finally {
            SpringApplication.exit(applicationContext, () -> exitCode[0]);
        }
    }

    private List<ImageMigrationRow> findNonGcsImages() {
        return jdbcTemplate.query(SELECT_NON_GCS_IMAGES, (rs, rowNum) -> new ImageMigrationRow(
                rs.getLong("id"),
                rs.getString("image_url"),
                rs.getString("source"),
                rs.getString("content_id")
        ));
    }

    private MigrationResult migrate(List<ImageMigrationRow> rows) {
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        long start = System.currentTimeMillis();

        for (int batchStart = 0; batchStart < rows.size(); batchStart += BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE, rows.size());
            List<ImageMigrationRow> batch = rows.subList(batchStart, batchEnd);

            List<UploadedImage> uploaded = uploadBatch(batch, failed);
            updateUrls(uploaded);
            success.addAndGet(uploaded.size());

            log.info("진행: {}/{} (성공 {}, 실패 {})",
                    batchEnd, rows.size(), success.get(), failed.get());
        }

        log.info("[측정] 마이그레이션 소요시간: {}ms", System.currentTimeMillis() - start);
        return new MigrationResult(success.get(), failed.get());
    }

    private List<UploadedImage> uploadBatch(List<ImageMigrationRow> batch, AtomicInteger failed) {
        List<UploadedImage> uploaded = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<UploadedImage>> futures = new ArrayList<>(batch.size());
            for (ImageMigrationRow row : batch) {
                futures.add(executor.submit(() -> {
                    String fileName = row.source() + "/" + row.contentId();
                    String gcsUrl = imageUploader.upload(row.imageUrl(), fileName);
                    return new UploadedImage(row.id(), gcsUrl);
                }));
            }

            for (Future<UploadedImage> future : futures) {
                try {
                    uploaded.add(future.get());
                } catch (Exception e) {
                    failed.incrementAndGet();
                    log.warn("이미지 업로드 실패: {}", e.getMessage());
                }
            }
        }

        return uploaded;
    }

    private void updateUrls(List<UploadedImage> uploaded) {
        if (uploaded.isEmpty()) {
            return;
        }

        var params = uploaded.stream()
                .map(img -> new MapSqlParameterSource()
                        .addValue("id", img.id())
                        .addValue("gcsUrl", img.gcsUrl()))
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(UPDATE_IMAGE_URL, params);
    }

    private record ImageMigrationRow(Long id, String imageUrl, String source, String contentId) {
    }

    private record UploadedImage(Long id, String gcsUrl) {
    }

    private record MigrationResult(int success, int failed) {
    }
}
