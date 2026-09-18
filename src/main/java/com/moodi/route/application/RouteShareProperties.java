package com.moodi.route.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 루트 공유 랜딩 페이지({@code GET /routes/shared/{publicId}})에 쓰는 값.
 *
 * <p>앱스토어 링크·서비스 로고는 배포 단계에 따라 바뀌므로 코드가 아니라 환경변수로 둔다.
 * 값이 비어 있으면(아직 앱스토어 미등록 등) 해당 기능만 조용히 생략한다 — 랜딩 페이지 자체는
 * 항상 동작해야 카카오톡 링크 미리보기가 깨지지 않는다.
 */
@ConfigurationProperties("moodi.route.share")
public record RouteShareProperties(
        String appScheme,
        String logoUrl,
        String iosStoreUrl,
        String androidStoreUrl
) {

    private static final String DEFAULT_APP_SCHEME = "moodi://route/";

    public RouteShareProperties {
        if (appScheme == null || appScheme.isBlank()) {
            appScheme = DEFAULT_APP_SCHEME;
        }
    }
}
