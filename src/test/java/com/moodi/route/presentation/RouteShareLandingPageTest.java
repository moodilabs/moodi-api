package com.moodi.route.presentation;

import com.moodi.route.application.RouteShareLandingView;
import com.moodi.route.domain.Route;
import com.moodi.route.domain.RouteDay;
import com.moodi.route.domain.RouteLeg;
import com.moodi.route.domain.RouteSpot;
import com.moodi.route.support.RouteFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RouteShareLandingPageTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Test
    @DisplayName("요약 줄은 '기간 · N days · N spots · 시/도' 형식이다")
    void describe_multi_day_route() {
        // given
        LocalDate start = LocalDate.of(2026, 9, 23);
        Route route = RouteFixture.createRoute(MEMBER_ID, "Seoul autumn walk", start, start.plusDays(3), List.of(
                RouteFixture.createDay(1, start, 2),
                RouteFixture.createDay(2, start.plusDays(1), 2),
                RouteFixture.createDay(3, start.plusDays(2), 1),
                RouteFixture.createDay(4, start.plusDays(3), 1)
        ));

        // when
        String description = RouteShareLandingPage.describe(RouteShareLandingView.from(route));

        // then
        assertThat(description).isEqualTo("Sep 23 – Sep 26 · 4 days · 6 spots · 서울");
    }

    @Test
    @DisplayName("당일 루트는 날짜 하나만, 단수는 day/spot 으로 표기한다")
    void describe_single_day_route() {
        // given
        LocalDate start = LocalDate.of(2026, 9, 23);
        Route route = RouteFixture.createRoute(MEMBER_ID, "Quick stop", start, start,
                List.of(RouteFixture.createDay(1, start, 1)));

        // when
        String description = RouteShareLandingPage.describe(RouteShareLandingView.from(route));

        // then
        assertThat(description).isEqualTo("Sep 23 · 1 day · 1 spot · 서울");
    }

    @Test
    @DisplayName("해가 바뀌는 기간은 연도를 붙인다")
    void format_date_range_across_years() {
        assertThat(RouteShareLandingPage.formatDateRange(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2)))
                .isEqualTo("Dec 30, 2026 – Jan 2, 2027");
    }

    @Test
    @DisplayName("시/도가 없는 스팟만 있으면 지역 항목을 생략한다")
    void describe_without_area() {
        // given
        LocalDate start = LocalDate.of(2026, 9, 23);
        RouteSpot spot = RouteSpot.create(1L, 1, 60, "Unknown place", null, null, null,
                37.5, 127.0, "TOURIST_ATTRACTION", null);
        RouteDay day = RouteDay.create(1, start, List.of(spot), List.<RouteLeg>of());
        Route route = RouteFixture.createRoute(MEMBER_ID, "No area", start, start, List.of(day));

        // when
        String description = RouteShareLandingPage.describe(RouteShareLandingView.from(route));

        // then
        assertThat(description).isEqualTo("Sep 23 · 1 day · 1 spot");
    }

    @Test
    @DisplayName("본문에 일차별 스팟 목록·썸네일·딥링크 버튼·스토어 버튼을 그린다")
    void render_full_page() {
        // given
        LocalDate start = LocalDate.of(2026, 9, 23);
        Route route = RouteFixture.createRoute(MEMBER_ID, "Seoul autumn walk", start, start.plusDays(1), List.of(
                RouteFixture.createDay(1, start, 2),
                RouteFixture.createDay(2, start.plusDays(1), 1)
        ));
        RouteShareLandingView view = RouteShareLandingView.from(route);
        RouteShareLandingPage page = new RouteShareLandingPage(
                "https://dev-api.moodi.kr/s/Ab12Cd34",
                "moodi://route/shared/" + route.getPublicId(),
                view.imageUrl(),
                "https://dev-api.moodi.kr/icon_ios.jpg",
                "https://apps.apple.com/app/moodi",
                "https://play.google.com/store/apps/details?id=com.mudi"
        );

        // when
        String html = page.render(view);

        // then
        assertThat(html)
                .contains("og:title\" content=\"Seoul autumn walk\"")
                .contains("og:description\" content=\"Sep 23 – Sep 24 · 2 days · 3 spots · 서울\"")
                .contains("og:image\" content=\"https://img.example.com/1.jpg\"")
                .contains("og:url\" content=\"https://dev-api.moodi.kr/s/Ab12Cd34\"")
                .contains("Day 1 <span class=\"day-date\">Sep 23</span>")
                .contains("Day 2 <span class=\"day-date\">Sep 24</span>")
                .contains("<span class=\"spot-name\">스팟 1</span>")
                .contains("<span class=\"spot-place\">성동구, 서울</span>")
                .contains("<img class=\"spot-thumb\" src=\"https://img.example.com/2.jpg\"")
                .contains("href=\"moodi://route/shared/" + route.getPublicId() + "\">Open in MOODI</a>")
                .contains("https://apps.apple.com/app/moodi")
                .contains("https://play.google.com/store/apps/details?id=com.mudi");
    }

    @Test
    @DisplayName("스토어 링크가 비어 있으면 스토어 버튼을 그리지 않는다")
    void render_hides_store_buttons_when_not_configured() {
        // given
        RouteShareLandingPage page = new RouteShareLandingPage(null, null, null,
                "https://dev-api.moodi.kr/icon_ios.jpg", "", null);

        // when
        String html = page.render(RouteShareLandingView.notFound());

        // then
        assertThat(html)
                .contains(RouteShareLandingPage.NOT_FOUND_HEADLINE)
                .contains("og:image\" content=\"https://dev-api.moodi.kr/icon_ios.jpg\"")
                .doesNotContain("App Store")
                .doesNotContain("Google Play")
                .doesNotContain("Open in MOODI");
    }

    @Test
    @DisplayName("http(s) 가 아닌 이미지 URL 은 그리지 않고 제목·스팟명의 마크업은 이스케이프한다")
    void render_escapes_and_drops_unsafe_urls() {
        // given
        LocalDate start = LocalDate.of(2026, 9, 23);
        RouteSpot spot = RouteSpot.create(1L, 1, 60, "<b>bold</b>", "javascript:alert(1)", "Seoul", "Jung-gu",
                37.5, 127.0, "TOURIST_ATTRACTION", null);
        RouteDay day = RouteDay.create(1, start, List.of(spot), List.<RouteLeg>of());
        Route route = RouteFixture.createRoute(MEMBER_ID, "<script>alert(1)</script>", start, start, List.of(day));
        RouteShareLandingView view = RouteShareLandingView.from(route);
        RouteShareLandingPage page = new RouteShareLandingPage(null, null, view.imageUrl(), null, null, null);

        // when
        String html = page.render(view);

        // then
        assertThat(html)
                .doesNotContain("<script>alert(1)</script>")
                .contains("&lt;script&gt;")
                .doesNotContain("javascript:alert(1)")
                .contains("&lt;b&gt;bold&lt;/b&gt;")
                .contains("spot-thumb-empty");
    }
}
