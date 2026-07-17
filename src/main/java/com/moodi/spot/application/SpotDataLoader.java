package com.moodi.spot.application;

import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotImage;
import com.moodi.spot.domain.SpotImageRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotDataLoader {

    private static final String LOCALE_KO = "ko-KR";

    private final SpotRepository spotRepository;
    private final SpotTranslationRepository spotTranslationRepository;
    private final SpotImageRepository spotImageRepository;

    public LoadResult load(List<SpotCsvRow> rows) {
        int saved = 0;
        int skipped = 0;
        int failed = 0;

        for (int i = 0; i < rows.size(); i++) {
            SpotCsvRow row = rows.get(i);
            int rowNumber = i + 2; // CSV 헤더가 1행이므로 데이터는 2행부터

            try {
                if (spotRepository.existsBySourceAndContentId(row.getSource(), row.getContentId())) {
                    skipped++;
                    continue;
                }

                Spot spot = saveSpot(row);
                saveTranslation(spot.getId(), row);
                saveImageIfPresent(spot.getId(), row);
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

    private Spot saveSpot(SpotCsvRow row) {
        SpotContentType contentType = SpotContentType.fromLabel(row.getContentType());

        return spotRepository.save(Spot.create(
                row.getContentId(),
                contentType,
                row.getArea(),
                row.getSource(),
                parseDouble(row.getLongitude()),
                parseDouble(row.getLatitude()),
                blankToNull(row.getTel()),
                blankToNull(row.getLclsSystm1()),
                blankToNull(row.getLclsSystm2()),
                blankToNull(row.getLclsSystm3())
        ));
    }

    private void saveTranslation(Long spotId, SpotCsvRow row) {
        spotTranslationRepository.save(SpotTranslation.create(
                spotId,
                LOCALE_KO,
                row.getTitle(),
                blankToNull(row.getOverview()),
                blankToNull(row.getAddr1()),
                blankToNull(row.getAddr2())
        ));
    }

    private void saveImageIfPresent(Long spotId, SpotCsvRow row) {
        String imageUrl = blankToNull(row.getSpotImage());
        if (imageUrl != null) {
            spotImageRepository.save(SpotImage.createPrimary(spotId, imageUrl));
        }
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

    public record LoadResult(int saved, int skipped, int failed) {
    }
}
