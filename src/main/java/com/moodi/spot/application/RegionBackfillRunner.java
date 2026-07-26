package com.moodi.spot.application;

import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotTranslation;
import com.moodi.spot.domain.SpotTranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "region-backfill.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RegionBackfillRunner implements ApplicationRunner {

    private static final String LOCALE_KO = "ko-KR";
    private static final int BATCH_SIZE = 500;

    private final SpotRepository spotRepository;
    private final SpotTranslationRepository spotTranslationRepository;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("지역 정규화 백필 시작");

        int[] exitCode = {0};
        try {
            BackfillResult result = backfill();
            log.info("지역 정규화 백필 결과: 전체 대상 {}건, district 성공 {}건, district 실패 {}건, " +
                            "neighborhood 성공 {}건, neighborhood 미존재 {}건",
                    result.total, result.districtSuccess, result.districtFail,
                    result.neighborhoodSuccess, result.neighborhoodMissing);

            if (result.districtFail > 0) {
                exitCode[0] = 1;
            }
        } catch (Exception e) {
            log.error("지역 정규화 백필 중 예외 발생: {}", e.getMessage(), e);
            exitCode[0] = 1;
        } finally {
            int code = SpringApplication.exit(applicationContext, () -> exitCode[0]);
            System.exit(code);
        }
    }

    @Transactional
    public BackfillResult backfill() {
        List<Spot> spots = spotRepository.findByDistrictIsNull();
        int total = spots.size();
        int districtSuccess = 0;
        int districtFail = 0;
        int neighborhoodSuccess = 0;
        int neighborhoodMissing = 0;

        for (int i = 0; i < spots.size(); i++) {
            Spot spot = spots.get(i);

            Optional<SpotTranslation> translationOpt =
                    spotTranslationRepository.findBySpotIdAndLocale(spot.getId(), LOCALE_KO);

            if (translationOpt.isEmpty()) {
                log.warn("한국어 번역 없음 — spotId={}", spot.getId());
                districtFail++;
                continue;
            }

            ParsedRegion region = RegionParser.parse(translationOpt.get().getAddr1());

            if (region.district() == null) {
                log.debug("district 추출 실패 — spotId={}, addr1={}",
                        spot.getId(), translationOpt.get().getAddr1());
                districtFail++;
            } else {
                districtSuccess++;
            }

            if (region.neighborhood() != null) {
                neighborhoodSuccess++;
            } else {
                neighborhoodMissing++;
            }

            spot.updateRegion(region.district(), region.neighborhood());

            if ((i + 1) % BATCH_SIZE == 0) {
                log.info("백필 진행 중: {}/{}", i + 1, total);
            }
        }

        return new BackfillResult(total, districtSuccess, districtFail,
                neighborhoodSuccess, neighborhoodMissing);
    }

    record BackfillResult(int total, int districtSuccess, int districtFail,
                          int neighborhoodSuccess, int neighborhoodMissing) {
    }
}
