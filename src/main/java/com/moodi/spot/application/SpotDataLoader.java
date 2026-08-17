package com.moodi.spot.application;

import com.moodi.spot.domain.SpotContentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class SpotDataLoader {

    private static final int CHUNK_SIZE = 1000;

    private final SpotBatchWriter spotBatchWriter;
    private final SpotImageUploader imageUploader;

    public SpotDataLoader(SpotBatchWriter spotBatchWriter, @Nullable SpotImageUploader imageUploader) {
        this.spotBatchWriter = spotBatchWriter;
        this.imageUploader = imageUploader;
    }

    public LoadResult load(List<SpotCsvRow> rows) {
        int inserted = 0;
        int updated = 0;
        int failed = 0;
        long saveTime = 0;
        int chunkSaveCount = 0;
        int fallbackCount = 0;

        for (int chunkStart = 0; chunkStart < rows.size(); chunkStart += CHUNK_SIZE) {
            int chunkEnd = Math.min(chunkStart + CHUNK_SIZE, rows.size());
            List<SpotCsvRow> chunk = rows.subList(chunkStart, chunkEnd);

            List<SpotImportRow> parsedRows = new ArrayList<>();
            for (SpotCsvRow row : chunk) {
                try {
                    parsedRows.add(parseRow(row));
                } catch (Exception e) {
                    failed++;
                    log.warn("스팟 파싱 실패 contentId={}: {}", row.getContentId(), e.getMessage());
                }
            }

            if (parsedRows.isEmpty()) {
                continue;
            }

            parsedRows = uploadImages(parsedRows);

            try {
                long t1 = System.currentTimeMillis();
                SpotBatchWriter.UpsertResult result = spotBatchWriter.upsertAll(parsedRows);
                saveTime += System.currentTimeMillis() - t1;
                inserted += result.inserted();
                updated += result.updated();
                chunkSaveCount++;
            } catch (Exception e) {
                log.warn("chunk 적재 실패 (행 {}~{}), 개별 저장으로 전환: {}",
                        chunkStart + 2, chunkEnd + 1, e.getMessage());
                fallbackCount++;

                for (SpotImportRow parsedRow : parsedRows) {
                    try {
                        long t2 = System.currentTimeMillis();
                        SpotBatchWriter.UpsertResult fallbackResult =
                                spotBatchWriter.upsertAll(List.of(parsedRow));
                        saveTime += System.currentTimeMillis() - t2;
                        inserted += fallbackResult.inserted();
                        updated += fallbackResult.updated();
                    } catch (Exception ex) {
                        failed++;
                        log.warn("스팟 적재 실패 contentId={}: {}", parsedRow.contentId(), ex.getMessage());
                    }
                }
            }
        }

        log.info("[측정] upsert: chunk 성공 {}회, fallback {}회, 총 {}ms",
                chunkSaveCount, fallbackCount, saveTime);
        log.info("스팟 적재 완료: 신규 {}건, 갱신 {}건, 실패 {}건, 전체 {}건",
                inserted, updated, failed, rows.size());
        return new LoadResult(inserted, updated, failed);
    }

    private List<SpotImportRow> uploadImages(List<SpotImportRow> rows) {
        if (imageUploader == null) {
            return rows;
        }

        long start = System.currentTimeMillis();
        int uploaded = 0;
        int skipped = 0;
        int uploadFailed = 0;

        List<SpotImportRow> result = new ArrayList<>(rows.size());
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<SpotImportRow>> futures = new ArrayList<>(rows.size());
            for (SpotImportRow row : rows) {
                futures.add(executor.submit(() -> uploadSingleImage(row)));
            }

            for (Future<SpotImportRow> future : futures) {
                try {
                    result.add(future.get());
                } catch (Exception e) {
                    uploadFailed++;
                }
            }
        }

        for (int i = 0; i < result.size(); i++) {
            SpotImportRow row = result.get(i);
            if (row.imageUrl() != null && row.imageUrl().startsWith("https://storage.googleapis.com/")) {
                uploaded++;
            } else if (row.imageUrl() == null) {
                skipped++;
            }
        }

        log.info("[측정] 이미지 업로드: {}건 성공, {}건 스킵, {}건 실패, 소요시간 {}ms",
                uploaded, skipped, uploadFailed, System.currentTimeMillis() - start);
        return result;
    }

    private SpotImportRow uploadSingleImage(SpotImportRow row) {
        if (row.imageUrl() == null || row.imageUrl().startsWith("https://storage.googleapis.com/")) {
            return row;
        }

        String fileName = row.source() + "/" + row.contentId();
        String gcsUrl = imageUploader.upload(row.imageUrl(), fileName);
        return row.withImageUrl(gcsUrl);
    }

    private SpotImportRow parseRow(SpotCsvRow row) {
        SpotContentType contentType = SpotContentType.fromLabel(row.getContentType());
        ParsedRegion region = RegionParser.parse(row.getAddr1());

        return new SpotImportRow(
                row.getContentId(),
                contentType.name(),
                row.getArea(),
                region.district(),
                region.neighborhood(),
                row.getSource(),
                parseDouble(row.getLongitude()),
                parseDouble(row.getLatitude()),
                blankToNull(row.getTel()),
                contentType.isRouteExcluded(),
                blankToNull(row.getLclsSystm1()),
                blankToNull(row.getLclsSystm2()),
                blankToNull(row.getLclsSystm3()),
                blankToNull(row.getHomepage()),
                row.getTitle(),
                blankToNull(row.getOverview()),
                blankToNull(row.getAddr1()),
                blankToNull(row.getAddr2()),
                blankToNull(row.getSpotImage())
        );
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public record LoadResult(int inserted, int updated, int failed) {
    }
}
