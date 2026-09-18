package com.moodi.route.presentation;

import com.moodi.route.application.RouteShareLandingView;
import com.moodi.route.application.RouteShareLandingView.DayView;
import com.moodi.route.application.RouteShareLandingView.SpotView;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 루트 공유 랜딩 페이지 HTML. 템플릿 엔진 없이 문자열로 만든다 — 페이지가 하나뿐이라 Thymeleaf 를
 * 들이는 비용이 더 크고, 카카오톡 크롤러가 읽는 건 어차피 {@code <head>} 의 OG 태그다.
 *
 * <p>사용자 데이터(제목·스팟명·이미지 URL)는 전부 {@link HtmlUtils#htmlEscape} 로 이스케이프하고,
 * 이미지 URL 은 http(s) 만 허용한다 — 스팟 스냅샷은 관리자가 넣는 값이지만 {@code javascript:} 같은
 * 스킴이 섞여도 페이지가 실행하지 않게.
 *
 * @param pageUrl         og:url·canonical. 단축 링크가 있으면 단축 링크
 * @param deepLink        "Open in MOODI" 버튼이 여는 앱 딥링크. 루트를 못 찾으면 null
 * @param imageUrl        og:image. 첫 스팟 이미지, 없으면 로고
 * @param iosStoreUrl     비어 있으면 버튼 생략
 * @param androidStoreUrl 비어 있으면 버튼 생략
 */
public record RouteShareLandingPage(
        String pageUrl,
        String deepLink,
        String imageUrl,
        String logoUrl,
        String iosStoreUrl,
        String androidStoreUrl
) {

    static final String SITE_NAME = "MOODI";
    static final String NOT_FOUND_HEADLINE = "This route is no longer available";
    static final String NOT_FOUND_DESCRIPTION = "Plan mood-based trips with MOODI.";

    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_DAY_YEAR = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    /** og:description 과 본문 요약 줄. 예) {@code Sep 23 – Sep 26 · 4 days · 6 spots · Seoul} */
    public static String describe(RouteShareLandingView view) {
        if (!view.found()) {
            return NOT_FOUND_DESCRIPTION;
        }
        List<String> parts = new ArrayList<>();
        parts.add(formatDateRange(view.startDate(), view.endDate()));
        parts.add(plural(view.totalDays(), "day"));
        parts.add(plural(view.spotCount(), "spot"));
        if (view.area() != null && !view.area().isBlank()) {
            parts.add(view.area());
        }
        return String.join(" · ", parts);
    }

    public static String formatDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return "";
        }
        if (start.equals(end)) {
            return start.format(MONTH_DAY);
        }
        if (start.getYear() == end.getYear()) {
            return start.format(MONTH_DAY) + " – " + end.format(MONTH_DAY);
        }
        return start.format(MONTH_DAY_YEAR) + " – " + end.format(MONTH_DAY_YEAR);
    }

    private static String plural(int count, String unit) {
        return count + " " + (count == 1 ? unit : unit + "s");
    }

    public String render(RouteShareLandingView view) {
        String title = esc(view.title());
        String description = esc(describe(view));

        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s · %s</title>
                <meta name="description" content="%s">
                %s\
                <style>
                %s\
                </style>
                </head>
                <body>
                <main class="page">
                %s\
                %s\
                </main>
                </body>
                </html>
                """;
        return html.formatted(title, SITE_NAME, description, metaTags(view, title, description), STYLE,
                view.found() ? routeSection(view, title, description) : notFoundSection(description),
                footer());
    }

    private String metaTags(RouteShareLandingView view, String title, String description) {
        StringBuilder meta = new StringBuilder();
        meta.append("<meta property=\"og:type\" content=\"website\">\n");
        meta.append("<meta property=\"og:site_name\" content=\"").append(SITE_NAME).append("\">\n");
        meta.append("<meta property=\"og:title\" content=\"").append(title).append("\">\n");
        meta.append("<meta property=\"og:description\" content=\"").append(description).append("\">\n");
        String image = safeHttpUrl(imageUrl) != null ? imageUrl : logoUrl;
        if (safeHttpUrl(image) != null) {
            meta.append("<meta property=\"og:image\" content=\"").append(esc(image)).append("\">\n");
            meta.append("<meta name=\"twitter:card\" content=\"summary_large_image\">\n");
        }
        if (pageUrl != null && !pageUrl.isBlank()) {
            meta.append("<meta property=\"og:url\" content=\"").append(esc(pageUrl)).append("\">\n");
            meta.append("<link rel=\"canonical\" href=\"").append(esc(pageUrl)).append("\">\n");
        }
        return meta.toString();
    }

    private String routeSection(RouteShareLandingView view, String title, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append(brandHeader());
        String hero = safeHttpUrl(imageUrl);
        if (hero != null) {
            sb.append("<img class=\"hero\" src=\"").append(esc(hero)).append("\" alt=\"\">\n");
        }
        sb.append("<h1 class=\"title\">").append(title).append("</h1>\n");
        sb.append("<p class=\"summary\">").append(description).append("</p>\n");
        sb.append(buttons(true));
        for (DayView day : view.days()) {
            sb.append(daySection(day));
        }
        return sb.toString();
    }

    private String notFoundSection(String description) {
        return brandHeader()
                + "<h1 class=\"title\">" + NOT_FOUND_HEADLINE + "</h1>\n"
                + "<p class=\"summary\">" + description + "</p>\n"
                + buttons(false);
    }

    private String brandHeader() {
        String logo = safeHttpUrl(logoUrl);
        String mark = logo != null
                ? "<img class=\"brand-mark\" src=\"" + esc(logo) + "\" alt=\"\">"
                : "";
        return "<header class=\"brand\">" + mark + "<span class=\"brand-name\">" + SITE_NAME + "</span></header>\n";
    }

    private String buttons(boolean withDeepLink) {
        StringBuilder sb = new StringBuilder("<div class=\"actions\">\n");
        if (withDeepLink && deepLink != null && !deepLink.isBlank()) {
            sb.append("<a class=\"btn btn-primary\" href=\"").append(esc(deepLink)).append("\">Open in MOODI</a>\n");
        }
        String ios = safeHttpUrl(iosStoreUrl);
        if (ios != null) {
            sb.append("<a class=\"btn btn-store\" href=\"").append(esc(ios))
                    .append("\" rel=\"noopener\">Download on the App Store</a>\n");
        }
        String android = safeHttpUrl(androidStoreUrl);
        if (android != null) {
            sb.append("<a class=\"btn btn-store\" href=\"").append(esc(android))
                    .append("\" rel=\"noopener\">Get it on Google Play</a>\n");
        }
        sb.append("</div>\n");
        return sb.toString();
    }

    private String daySection(DayView day) {
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"day\">\n");
        sb.append("<h2 class=\"day-title\">Day ").append(day.dayNumber());
        if (day.date() != null) {
            sb.append(" <span class=\"day-date\">").append(day.date().format(MONTH_DAY)).append("</span>");
        }
        sb.append("</h2>\n<ol class=\"spots\">\n");
        for (SpotView spot : day.spots()) {
            sb.append(spotRow(spot));
        }
        sb.append("</ol>\n</section>\n");
        return sb.toString();
    }

    private String spotRow(SpotView spot) {
        String thumb = safeHttpUrl(spot.imageUrl());
        String media = thumb != null
                ? "<img class=\"spot-thumb\" src=\"" + esc(thumb) + "\" alt=\"\" loading=\"lazy\">"
                : "<span class=\"spot-thumb spot-thumb-empty\">" + spot.sequence() + "</span>";
        String place = joinNonBlank(spot.district(), spot.area());
        return "<li class=\"spot\">" + media
                + "<div class=\"spot-body\"><span class=\"spot-name\">" + esc(spot.title()) + "</span>"
                + (place.isEmpty() ? "" : "<span class=\"spot-place\">" + esc(place) + "</span>")
                + "</div></li>\n";
    }

    private static String footer() {
        return "<footer class=\"foot\">Shared with " + SITE_NAME + "</footer>\n";
    }

    private static String joinNonBlank(String a, String b) {
        boolean hasA = a != null && !a.isBlank();
        boolean hasB = b != null && !b.isBlank();
        if (hasA && hasB) {
            return a + ", " + b;
        }
        return hasA ? a : (hasB ? b : "");
    }

    private static String safeHttpUrl(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return trimmed;
        }
        return null;
    }

    /** UTF-8 모드 — 기본(ISO-8859-1)은 "–"·"·" 까지 엔티티로 바꿔 og:description 이 지저분해진다. */
    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name());
    }

    /** 라이트/다크 모두 무난한 최소 스타일. 외부 폰트·스크립트 없음 — 앱 미설치 환경에서도 그대로 뜬다. */
    private static final String STYLE = """
            :root { color-scheme: light dark; --bg: #f7f7f8; --card: #ffffff; --fg: #17171b; --muted: #6b6b76;
                    --line: #e6e6ea; --accent: #2f6df6; --accent-fg: #ffffff; }
            @media (prefers-color-scheme: dark) {
              :root { --bg: #111114; --card: #1b1b20; --fg: #f2f2f5; --muted: #a0a0ab; --line: #2a2a31; }
            }
            * { box-sizing: border-box; }
            body { margin: 0; background: var(--bg); color: var(--fg);
                   font: 16px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Apple SD Gothic Neo",
                   "Noto Sans KR", sans-serif; -webkit-text-size-adjust: 100%; }
            .page { max-width: 520px; margin: 0 auto; padding: 20px 16px 40px; }
            .brand { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
            .brand-mark { width: 28px; height: 28px; border-radius: 8px; object-fit: cover; }
            .brand-name { font-weight: 700; letter-spacing: .08em; font-size: 14px; color: var(--muted); }
            .hero { display: block; width: 100%; aspect-ratio: 16 / 10; object-fit: cover; border-radius: 16px;
                    background: var(--line); }
            .title { font-size: 24px; line-height: 1.3; margin: 16px 0 4px; word-break: keep-all; overflow-wrap: anywhere; }
            .summary { margin: 0 0 16px; color: var(--muted); }
            .actions { display: flex; flex-direction: column; gap: 8px; margin: 0 0 24px; }
            .btn { display: block; text-align: center; padding: 14px 16px; border-radius: 12px; font-weight: 600;
                   text-decoration: none; border: 1px solid var(--line); background: var(--card); color: var(--fg); }
            .btn-primary { background: var(--accent); color: var(--accent-fg); border-color: var(--accent); }
            .day { margin-bottom: 20px; }
            .day-title { font-size: 15px; margin: 0 0 8px; }
            .day-date { font-weight: 400; color: var(--muted); margin-left: 6px; }
            .spots { list-style: none; margin: 0; padding: 0; background: var(--card); border: 1px solid var(--line);
                     border-radius: 14px; overflow: hidden; }
            .spot { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-top: 1px solid var(--line); }
            .spot:first-child { border-top: 0; }
            .spot-thumb { flex: none; width: 56px; height: 56px; border-radius: 10px; object-fit: cover; background: var(--line); }
            .spot-thumb-empty { display: inline-flex; align-items: center; justify-content: center; color: var(--muted);
                                font-weight: 600; }
            .spot-body { display: flex; flex-direction: column; min-width: 0; }
            .spot-name { font-weight: 600; overflow-wrap: anywhere; }
            .spot-place { font-size: 13px; color: var(--muted); }
            .foot { margin-top: 32px; font-size: 12px; color: var(--muted); text-align: center; }
            """;
}
