package com.moodi.route.presentation;

import com.moodi.route.application.RouteShareLandingService;
import com.moodi.route.application.RouteShareLandingView;
import com.moodi.route.application.RouteShareLinkBuilder;
import com.moodi.route.application.RouteShareProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 루트 공유 랜딩 페이지. 두 주소가 같은 페이지를 그린다.
 *
 * <ul>
 *   <li>{@code GET /s/{code}} — 단축 링크. 카카오톡 카드·OS 공유 문구에 들어가는 주소</li>
 *   <li>{@code GET /routes/shared/{publicId}} — 단축 코드 도입 전에 퍼진 긴 링크. 계속 동작해야 한다</li>
 * </ul>
 *
 * <p>카카오톡 등 외부 메신저 크롤러는 이 URL의 OG 메타태그만 읽고 JS는 실행하지 않는다. 반대로 실제
 * 사용자는 이 페이지에서 루트를 훑어보고 "Open in MOODI" 로 앱에 들어간다. 두 경우 모두
 * {@code GlobalExceptionHandler}가 이 요청을 가로채 JSON(application/problem+json)으로 응답해버리면
 * 미리보기 카드도, 딥링크 이동도 깨진다 — 그래서 존재하지 않는/삭제된 루트여도 예외를 던지지 않고
 * 항상 200 + HTML로 응답한다. 단축 링크를 302 로 긴 링크에 넘기지 않고 직접 그리는 것도 같은 이유다:
 * 리다이렉트를 따라가지 않는 크롤러가 있다.
 */
@RestController
public class RouteShareLandingController {

    /** 루트 편집이 카드에 반영되기까지 허용하는 지연. Firebase Hosting CDN 도 이 값을 그대로 따른다. */
    private static final Duration FOUND_MAX_AGE = Duration.ofMinutes(5);

    private final RouteShareLandingService routeShareLandingService;
    private final RouteShareLinkBuilder shareLinkBuilder;
    private final RouteShareProperties properties;

    public RouteShareLandingController(RouteShareLandingService routeShareLandingService,
                                        RouteShareLinkBuilder shareLinkBuilder,
                                        RouteShareProperties properties) {
        this.routeShareLandingService = routeShareLandingService;
        this.shareLinkBuilder = shareLinkBuilder;
        this.properties = properties;
    }

    @GetMapping(value = "/routes/shared/{publicId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> landing(@PathVariable String publicId, HttpServletRequest request) {
        RouteShareLandingView view = parsePublicId(publicId)
                .map(routeShareLandingService::getLandingView)
                .orElseGet(RouteShareLandingView::notFound);
        return respond(view, request);
    }

    @GetMapping(value = "/s/{code}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shortLink(@PathVariable String code, HttpServletRequest request) {
        RouteShareLandingView view = routeShareLandingService.getLandingViewByShortCode(code);
        return respond(view, request);
    }

    private ResponseEntity<String> respond(RouteShareLandingView view, HttpServletRequest request) {
        String origin = RequestOrigin.of(request);
        String logoUrl = resolveLogoUrl(origin);

        RouteShareLandingPage page = new RouteShareLandingPage(
                view.found() ? shareLinkBuilder.shareUrl(origin, view.publicId(), view.shortCode()) : null,
                view.found() ? shareLinkBuilder.deepLink(view.publicId()) : null,
                view.imageUrl() != null ? view.imageUrl() : logoUrl,
                logoUrl,
                properties.iosStoreUrl(),
                properties.androidStoreUrl()
        );

        CacheControl cacheControl = view.found()
                ? CacheControl.maxAge(FOUND_MAX_AGE).cachePublic()
                : CacheControl.noStore();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
                .cacheControl(cacheControl)
                .body(page.render(view));
    }

    /**
     * 설정값(moodi.route.share.logo-url)이 비어 있으면 이 페이지와 같은 호스트의
     * 정적 리소스(icon_ios.jpg)로 폴백한다. moodi.kr 같은 도메인을 코드에 고정해두면
     * 그 도메인이 이 API가 아닌 다른 프론트로 옮겨갔을 때 og:image가 조용히 깨진다 —
     * 실제로 이 문제로 카카오톡 미리보기 카드 자체가 안 뜨는 걸 배포 후 확인했다.
     */
    private String resolveLogoUrl(String origin) {
        String configured = properties.logoUrl();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return origin + "/icon_ios.jpg";
    }

    private Optional<UUID> parsePublicId(String publicId) {
        try {
            return Optional.of(UUID.fromString(publicId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
