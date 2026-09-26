package com.moodi.spot.infrastructure.persistence;

import com.moodi.spot.domain.MoodTaggingStatus;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotRepository.MoodTaggingStatusCount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;

class SpotTaggingQueryRepositoryImpl implements SpotTaggingQueryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<Spot> findTaggingTargets(LocalDateTime now, int limit) {
        return em.createQuery("""
                        SELECT s FROM Spot s
                        WHERE s.status = com.moodi.spot.domain.SpotStatus.TAGGING_PENDING
                          AND s.routeExcluded = false
                          AND (s.moodTaggingStatus = com.moodi.spot.domain.MoodTaggingStatus.PENDING
                               OR (s.moodTaggingStatus = com.moodi.spot.domain.MoodTaggingStatus.RETRY_WAIT
                                   AND s.moodTaggingNextRetryAt <= :now))
                        ORDER BY s.moodTaggingStatus ASC, s.id ASC
                        """, Spot.class)
                .setParameter("now", now)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Spot> findStaleProcessing(LocalDateTime threshold) {
        return em.createQuery("""
                        SELECT s FROM Spot s
                        WHERE s.moodTaggingStatus = com.moodi.spot.domain.MoodTaggingStatus.PROCESSING
                          AND s.moodTaggingProcessingStartedAt < :threshold
                        """, Spot.class)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MoodTaggingStatusCount> countByMoodTaggingStatus() {
        List<Object[]> rows = em.createQuery("""
                        SELECT s.moodTaggingStatus, COUNT(s)
                        FROM Spot s
                        WHERE s.status = com.moodi.spot.domain.SpotStatus.TAGGING_PENDING
                           OR s.moodTaggingStatus = com.moodi.spot.domain.MoodTaggingStatus.COMPLETED
                        GROUP BY s.moodTaggingStatus
                        """)
                .getResultList();
        return rows.stream()
                .map(row -> new MoodTaggingStatusCount((MoodTaggingStatus) row[0], (long) row[1]))
                .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Spot> findByMoodTaggingStatus(MoodTaggingStatus moodTaggingStatus) {
        return em.createQuery("""
                        SELECT s FROM Spot s
                        WHERE s.moodTaggingStatus = :status
                        ORDER BY s.moodTaggingLastAttemptedAt DESC
                        """, Spot.class)
                .setParameter("status", moodTaggingStatus)
                .getResultList();
    }
}
