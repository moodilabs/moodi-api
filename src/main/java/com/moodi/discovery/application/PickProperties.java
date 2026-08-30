package com.moodi.discovery.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 유사도 정렬은 자바에서 하므로 후보를 무한정 읽으면 메모리와 응답 시간이 함께 늘어난다.
 * 지역이 넓게 잡히면 후보가 크게 불어날 수 있어 상한을 설정값으로 둔다.
 */
@ConfigurationProperties("moodi.pick")
public record PickProperties(int candidateLimit) {

    private static final int DEFAULT_CANDIDATE_LIMIT = 500;

    public PickProperties {
        if (candidateLimit <= 0) {
            candidateLimit = DEFAULT_CANDIDATE_LIMIT;
        }
    }
}
