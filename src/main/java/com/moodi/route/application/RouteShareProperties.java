package com.moodi.route.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 루트 공유 링크·랜딩 페이지({@code GET /routes/shared/{publicId}}, {@code GET /s/{code}})에 쓰는 값.
 *
 * <p>앱스토어 링크·서비스 로고·공개 도메인은 배포 단계에 따라 바뀌므로 코드가 아니라 환경변수로 둔다.
 * 값이 비어 있으면(아직 앱스토어 미등록 등) 해당 기능만 조용히 생략한다 — 랜딩 페이지 자체는
 * 항상 동작해야 카카오톡 링크 미리보기가 깨지지 않는다.
 *
 * @param appScheme 딥링크 접두사. 앱의 {@code parseSharedRouteLink} 는 {@code route/shared/{publicId}} 형태만
 *                  인식하므로 {@code moodi://route/} 로 두면 앱이 열리기만 하고 루트 화면으로 가지 않는다.
 * @param baseUrl   공유 링크의 공개 origin. 비우면 요청이 들어온 호스트를 쓴다({@link RouteShareLinkBuilder}).
 */
@ConfigurationProperties("moodi.route.share")
public record RouteShareProperties(
        String appScheme,
        String baseUrl,
        String logoUrl,
        String iosStoreUrl,
        String androidStoreUrl
) {

    private static final String DEFAULT_APP_SCHEME = "moodi://route/shared/";

    public RouteShareProperties {
        if (appScheme == null || appScheme.isBlank()) {
            appScheme = DEFAULT_APP_SCHEME;
        }
    }
}
