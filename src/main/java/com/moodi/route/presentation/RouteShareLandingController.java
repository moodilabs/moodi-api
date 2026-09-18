package com.moodi.route.presentation;

import com.moodi.route.application.RouteShareLandingService;
import com.moodi.route.application.RouteShareLandingView;
import com.moodi.route.application.RouteShareProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * 카카오톡 등 외부 메신저 크롤러는 이 URL의 OG 메타태그만 읽고 JS는 실행하지 않는다. 반대로 실제
 * 사용자는 이 페이지에서 앱 딥링크로 바로 튕겨나가야 한다. 두 경우 모두 {@code GlobalExceptionHandler}가
 * 이 요청을 가로채 JSON(application/problem+json)으로 응답해버리면 미리보기 카드도, 딥링크 이동도
 * 깨진다 — 그래서 여기서는 존재하지 않는/삭제된 루트여도 예외를 던지지 않고 항상 200 + HTML로 응답한다.
 */
@RestController
public class RouteShareLandingController {

    private final RouteShareLandingService routeShareLandingService;
    private final RouteShareProperties properties;

    public RouteShareLandingController(RouteShareLandingService routeShareLandingService,
                                        RouteShareProperties properties) {
        this.routeShareLandingService = routeShareLandingService;
        this.properties = properties;
    }

    @GetMapping(value = "/routes/shared/{publicId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> landing(
            @PathVariable String publicId,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {

        RouteShareLandingView view = parsePublicId(publicId)
                .map(routeShareLandingService::getLandingView)
                .orElseGet(RouteShareLandingView::notFound);

        String deepLink = properties.appScheme() + publicId;
        Platform platform = Platform.from(userAgent);
        String storeUrl = platform.storeUrl(properties);

        String html = render(view, deepLink, platform, storeUrl);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
                .body(html);
    }

    private Optional<UUID> parsePublicId(String publicId) {
        try {
            return Optional.of(UUID.fromString(publicId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String render(RouteShareLandingView view, String deepLink, Platform platform, String storeUrl) {
        String safeTitle = HtmlUtils.htmlEscape(view.title());
        String logoUrl = properties.logoUrl();

        StringBuilder meta = new StringBuilder();
        meta.append("<meta property=\"og:type\" content=\"website\">\n");
        meta.append("<meta property=\"og:title\" content=\"").append(safeTitle).append("\">\n");
        if (logoUrl != null && !logoUrl.isBlank()) {
            meta.append("<meta property=\"og:image\" content=\"")
                    .append(HtmlUtils.htmlEscape(logoUrl)).append("\">\n");
        }

        String redirectScript = platform == Platform.OTHER
                ? ""
                : buildRedirectScript(deepLink, storeUrl);

        String bodyMessage = platform == Platform.OTHER
                ? "Moodi 앱에서 루트를 확인해 보세요."
                : "Moodi 앱으로 이동 중입니다...";

        return """
                <!doctype html>
                <html lang="ko">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                %s\
                <title>%s</title>
                </head>
                <body>
                <p>%s</p>
                %s\
                </body>
                </html>
                """.formatted(meta, safeTitle, bodyMessage, redirectScript);
    }

    private String buildRedirectScript(String deepLink, String storeUrl) {
        String storeRedirect = (storeUrl == null || storeUrl.isBlank())
                ? ""
                : """
                  setTimeout(function () {
                    window.location.href = "%s";
                  }, 1500);
                  """.formatted(storeUrl);

        return """
                <script>
                  window.location.href = "%s";
                  %s\
                </script>
                """.formatted(deepLink, storeRedirect);
    }

    private enum Platform {
        IOS, ANDROID, OTHER;

        static Platform from(String userAgent) {
            if (userAgent == null) {
                return OTHER;
            }
            String ua = userAgent.toLowerCase();
            if (ua.contains("android")) {
                return ANDROID;
            }
            if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
                return IOS;
            }
            return OTHER;
        }

        String storeUrl(RouteShareProperties properties) {
            return switch (this) {
                case IOS -> properties.iosStoreUrl();
                case ANDROID -> properties.androidStoreUrl();
                case OTHER -> null;
            };
        }
    }
}
