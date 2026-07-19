package com.moodi.spot.application;

import com.moodi.spot.domain.SpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotDataLoader {

    private final SpotRepository spotRepository;
    private final SpotRowSaver spotRowSaver;

    public LoadResult load(List<SpotCsvRow> rows) {
        int saved = 0;
        int skipped = 0;
        int failed = 0;

        for (int i = 0; i < rows.size(); i++) {
            SpotCsvRow row = rows.get(i);
            int rowNumber = i + 2;

            try {
                if (spotRepository.existsBySourceAndContentId(row.getSource(), row.getContentId())) {
                    skipped++;
                    continue;
                }

                spotRowSaver.save(row);
                saved++;
            } catch (Exception e) {
                failed++;
                log.warn("스팟 적재 실패 [행 {}] contentId={}, source={}: {}",
                        rowNumber, row.getContentId(), row.getSource(), e.getMessage());
            }
        }

        log.info("스팟 적재 완료: 저장 {}건, 스킵 {}건, 실패 {}건, 전체 {}건",
                saved, skipped, failed, rows.size());
        return new LoadResult(saved, skipped, failed);
    }

    public record LoadResult(int saved, int skipped, int failed) {
    }
}
