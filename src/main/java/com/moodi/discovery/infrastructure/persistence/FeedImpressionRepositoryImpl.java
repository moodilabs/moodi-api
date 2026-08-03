package com.moodi.discovery.infrastructure.persistence;

import com.moodi.discovery.application.FeedImpressionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
public class FeedImpressionRepositoryImpl implements FeedImpressionRepository {

    private final EntityManager em;

    public FeedImpressionRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public void recordShown(UUID memberId, List<Long> spotIds, LocalDateTime shownAt) {
        if (spotIds.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO feed_impression (member_id, spot_id, shown_at) VALUES "
                + valuesClause(spotIds.size())
                + " ON CONFLICT (member_id, spot_id) DO UPDATE SET shown_at = EXCLUDED.shown_at";

        Query query = em.createNativeQuery(sql)
                .setParameter("memberId", memberId)
                .setParameter("shownAt", shownAt);
        IntStream.range(0, spotIds.size())
                .forEach(i -> query.setParameter("spotId" + i, spotIds.get(i)));

        query.executeUpdate();
    }

    private String valuesClause(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> "(:memberId, :spotId" + i + ", :shownAt)")
                .collect(Collectors.joining(", "));
    }
}
