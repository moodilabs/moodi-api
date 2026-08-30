package com.moodi.discovery.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Pick 사진은 개인 사진이라 스팟 이미지 버킷({@code moodi-spot-images})을 함께 쓸 수 없다.
 * 그쪽은 공개 URL을 만들어 내보내는 용도라 사용자 사진이 그대로 열람 가능해진다.
 * 그래서 비공개 버킷을 따로 받는다.
 */
@ConfigurationProperties("gcs.pick-image")
public record PickImageProperties(boolean enabled, String bucket, long uploadUrlTtlSeconds) {

    private static final long DEFAULT_TTL_SECONDS = 300;

    public PickImageProperties {
        if (uploadUrlTtlSeconds <= 0) {
            uploadUrlTtlSeconds = DEFAULT_TTL_SECONDS;
        }
    }
}
