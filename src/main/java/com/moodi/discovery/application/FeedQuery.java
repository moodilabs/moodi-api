package com.moodi.discovery.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 피드 후보 조회 조건. {@code cursor}가 없으면 첫 페이지다.
 *
 * @param impressionFrom 이 시각 이후의 노출만 "본 것"으로 친다 (유효 기간 경계)
 * @param sessionAt      이 시각 이하의 노출만 "본 것"으로 친다 (스크롤 중 순서 고정)
 */
public record FeedQuery(
        UUID memberId,
        List<String> moodTagKeys,
        String seed,
        LocalDateTime sessionAt,
        LocalDateTime impressionFrom,
        FeedCursor cursor,
        int limit
) {
}
