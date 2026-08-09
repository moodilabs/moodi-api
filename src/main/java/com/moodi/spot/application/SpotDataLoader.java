package com.moodi.spot.application;

import com.moodi.spot.domain.SpotContentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotDataLoader {

    private static final int CHUNK_SIZE = 1000;

    private final SpotBatchWriter spotBatchWriter;
    private final SpotRowSaver spotRowSaver;

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

                for (SpotCsvRow row : chunk) {
                    try {
                        long t2 = System.currentTimeMillis();
                        spotRowSaver.saveRow(row);
                        saveTime += System.currentTimeMillis() - t2;
                        inserted++;
                    } catch (Exception ex) {
                        failed++;
                        log.warn("스팟 적재 실패 contentId={}: {}", row.getContentId(), ex.getMessage());
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
