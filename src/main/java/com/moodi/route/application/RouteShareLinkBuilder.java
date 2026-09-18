package com.moodi.route.application;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 루트 공유에 쓰는 세 가지 주소를 한곳에서 만든다.
 *
 * <ul>
 *   <li>단축 링크 {@code {base}/s/{code}} — 카카오톡 카드·문자 등 외부로 나가는 주소</li>
 *   <li>긴 링크 {@code {base}/routes/shared/{publicId}} — 단축 코드가 아직 없는 예전 공유 루트용 폴백</li>
 *   <li>앱 딥링크 {@code moodi://route/shared/{publicId}}</li>
 * </ul>
 *
 * <p>base는 설정({@code moodi.route.share.base-url})이 있으면 그 값을, 없으면 요청이 들어온 호스트를
 * 쓴다. 도메인을 코드에 고정하면 dev-api.moodi.kr / moodi.kr 처럼 환경마다 다른 호스트에서 링크가
 * 엉뚱한 곳을 가리키게 된다 — 랜딩 페이지 og:image 가 같은 이유로 깨진 전례가 있다.
 */
@Component
public class RouteShareLinkBuilder {

    private static final String SHARED_ROUTE_PATH = "/routes/shared/";
    private static final String SHORT_LINK_PATH = "/s/";

    private final RouteShareProperties properties;

    public RouteShareLinkBuilder(RouteShareProperties properties) {
        this.properties = properties;
    }

    /** 단축 코드가 있으면 단축 링크, 없으면 긴 링크. 응답의 {@code shareUrl} 은 항상 이걸로 채운다. */
    public String shareUrl(String requestOrigin, UUID publicId, String shortCode) {
        if (shortCode == null || shortCode.isBlank()) {
            return longUrl(requestOrigin, publicId);
        }
        return shortUrl(requestOrigin, shortCode);
    }

    public String shortUrl(String requestOrigin, String shortCode) {
        return baseUrl(requestOrigin) + SHORT_LINK_PATH + shortCode;
    }

    public String longUrl(String requestOrigin, UUID publicId) {
        return baseUrl(requestOrigin) + SHARED_ROUTE_PATH + publicId;
    }

    public String deepLink(UUID publicId) {
        return properties.appScheme() + publicId;
    }

    private String baseUrl(String requestOrigin) {
        String configured = properties.baseUrl();
        String base = (configured != null && !configured.isBlank()) ? configured : requestOrigin;
        return stripTrailingSlash(base);
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
