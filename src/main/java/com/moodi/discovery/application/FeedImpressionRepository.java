package com.moodi.discovery.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 피드 노출 이력. 추천 컨텍스트 자신의 데이터이고 쓰기가 upsert 하나뿐이라
 * JPA 엔티티 없이 조회·기록 포트로만 둔다.
 */
public interface FeedImpressionRepository {

    /**
     * 이미 본 스팟이면 {@code shown_at}만 갱신한다. 재노출 주기는 이 시각을 기준으로 다시 계산된다.
     */
    void recordShown(UUID memberId, List<Long> spotIds, LocalDateTime shownAt);
}
