package com.moodi.spot.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.spot.domain.MoodTaggingStatus;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotRepository;
import com.moodi.spot.domain.SpotRepository.MoodTaggingStatusCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class MoodTaggingAdminService {

    private final SpotRepository spotRepository;

    public MoodTaggingAdminService(SpotRepository spotRepository) {
        this.spotRepository = spotRepository;
    }

    public Map<String, Long> getSummary() {
        List<MoodTaggingStatusCount> counts = spotRepository.countByMoodTaggingStatus();
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (MoodTaggingStatusCount count : counts) {
            statusCounts.put(count.status().name(), count.count());
        }
        return statusCounts;
    }

    public List<Spot> getFailed() {
        return spotRepository.findByMoodTaggingStatus(MoodTaggingStatus.FAILED);
    }

    @Transactional
    public void retrySpot(Long spotId) {
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SPOT_NOT_FOUND));
        spot.resetForRetry();
        spotRepository.save(spot);
    }
}
