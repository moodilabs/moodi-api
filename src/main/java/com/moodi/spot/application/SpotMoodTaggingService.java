package com.moodi.spot.application;

import java.util.List;

import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotMoodRepository;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotMoodTaggingService {

    private final SpotRepository spotRepository;
    private final SpotMoodRepository spotMoodRepository;
    private final SpotMoodTagger spotMoodTagger;

    public TaggingResult tagAll(int limit) {
        List<Spot> pendingSpots = spotRepository.findByStatusAndRouteExcluded(SpotStatus.TAGGING_PENDING, false);
        if (limit > 0) {
            pendingSpots = pendingSpots.subList(0, Math.min(limit, pendingSpots.size()));
        }
        log.info("태깅 대상 스팟 {}건 조회 (limit={})", pendingSpots.size(), limit);

        int tagged = 0;
        int skipped = 0;
        int failed = 0;
        int invalidResponses = 0;

        for (Spot spot : pendingSpots) {
            try {
                if (spotMoodRepository.existsBySpotId(spot.getId())) {
                    skipped++;
                    continue;
                }

                spotMoodTagger.tagSpot(spot);
                tagged++;
                log.info("태깅 성공 spotId={} ({}/{})", spot.getId(), tagged, pendingSpots.size());
            } catch (Exception e) {
                failed++;
                if (e.getMessage() != null && e.getMessage().contains("invalid")) {
                    invalidResponses++;
                }
                log.warn("태깅 실패 spotId={}: {}", spot.getId(), e.getMessage());
            }
        }

        log.info("태깅 완료: 성공 {}건, 스킵 {}건, 실패 {}건 (invalid 응답 {}건 / 전체 요청 {}건)",
                tagged, skipped, failed, invalidResponses, tagged + failed);
        return new TaggingResult(tagged, skipped, failed, invalidResponses);
    }

    public record TaggingResult(int tagged, int skipped, int failed, int invalidResponses) {}
}
