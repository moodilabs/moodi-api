package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.domain.MoodTaggingStatus;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotRepository.MoodTaggingStatusCount;

import java.time.LocalDateTime;
import java.util.List;

interface SpotTaggingQueryRepository {

    List<Spot> findTaggingTargets(LocalDateTime now, int limit);

    List<Spot> findStaleProcessing(LocalDateTime threshold);

    List<MoodTaggingStatusCount> countByMoodTaggingStatus();

    List<Spot> findByMoodTaggingStatus(MoodTaggingStatus moodTaggingStatus);
}
