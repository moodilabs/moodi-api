package com.moodi.spot.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpotRepository {

    Spot save(Spot spot);

    Optional<Spot> findById(Long id);

    Optional<Spot> findBySourceAndContentId(String source, String contentId);

    boolean existsBySourceAndContentId(String source, String contentId);

    List<Spot> findBySourceAndContentIdIn(String source, List<String> contentIds);

    List<Spot> findByStatus(SpotStatus status);

    List<Spot> findByStatusAndRouteExcluded(SpotStatus status, boolean routeExcluded);

    List<Spot> findByDistrictIsNull();

    List<Spot> findByIdIn(List<Long> ids);

    /** PENDING 또는 재시도 시각이 지난 RETRY_WAIT 스팟을 조회한다. */
    List<Spot> findTaggingTargets(LocalDateTime now, int limit);

    /** PROCESSING 상태가 일정 시간 이상 지속된 스팟을 조회한다. */
    List<Spot> findStaleProcessing(LocalDateTime threshold);

    /** 태깅 상태별 건수를 조회한다. */
    List<MoodTaggingStatusCount> countByMoodTaggingStatus();

    /** FAILED 상태인 스팟을 조회한다. */
    List<Spot> findByMoodTaggingStatus(MoodTaggingStatus moodTaggingStatus);

    record MoodTaggingStatusCount(MoodTaggingStatus status, long count) {}
}
