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

    /**
     * 한 문장의 {@code ON CONFLICT DO UPDATE}가 같은 충돌 대상을 두 번 건드리면 Postgres가
     * "command cannot affect row a second time"로 실패한다. 지금은 피드 쿼리의 조인이 전부
     * 유니크 제약으로 1:1이라 중복이 올라올 일이 없지만, 상위 쿼리가 바뀌면 500으로 드러나는
     * 실패라 여기서 막는다.
     */
    @Override
    public void recordShown(UUID memberId, List<Long> spotIds, LocalDateTime shownAt) {
        List<Long> distinctSpotIds = spotIds.stream().distinct().toList();
        if (distinctSpotIds.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO feed_impression (member_id, spot_id, shown_at) VALUES "
                + valuesClause(distinctSpotIds.size())
                + " ON CONFLICT (member_id, spot_id) DO UPDATE SET shown_at = EXCLUDED.shown_at";

        Query query = em.createNativeQuery(sql)
                .setParameter("memberId", memberId)
                .setParameter("shownAt", shownAt);
        IntStream.range(0, distinctSpotIds.size())
                .forEach(i -> query.setParameter("spotId" + i, distinctSpotIds.get(i)));

        query.executeUpdate();
    }

    private String valuesClause(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> "(:memberId, :spotId" + i + ", :shownAt)")
                .collect(Collectors.joining(", "));
    }
}
