package com.moodi.discovery.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 노출 이력 유효 기간은 스팟 카탈로그 규모에 따라 조정 대상이라 설정값으로 뺀다.
 * 카탈로그가 크면 재순환 압박이 없으므로 길게 잡아 재등장을 줄인다.
 */
@ConfigurationProperties("moodi.feed")
public record FeedProperties(int impressionWindowDays) {

    private static final int DEFAULT_IMPRESSION_WINDOW_DAYS = 30;

    public FeedProperties {
        if (impressionWindowDays <= 0) {
            impressionWindowDays = DEFAULT_IMPRESSION_WINDOW_DAYS;
        }
    }
}
