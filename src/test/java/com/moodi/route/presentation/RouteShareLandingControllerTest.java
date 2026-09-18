package com.moodi.route.presentation;

import com.moodi.route.application.RouteShareLandingService;
import com.moodi.route.application.RouteShareLandingView;
import com.moodi.route.application.RouteShareLinkBuilder;
import com.moodi.route.application.RouteShareProperties;
import com.moodi.route.domain.Route;
import com.moodi.route.support.RouteFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RouteShareLandingControllerTest {

    @Mock
    private RouteShareLandingService routeShareLandingService;

    private MockMvc mockMvc;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 9, 23);
    private static final String APP_SCHEME = "moodi://route/shared/";
    private static final String LOGO_URL = "https://moodi.kr/logo.png";
    private static final String IOS_STORE_URL = "https://apps.apple.com/app/moodi";
    private static final String ANDROID_STORE_URL = "https://play.google.com/store/apps/details?id=com.mudi";

    @BeforeEach
    void setUp() {
        mockMvc = mockMvcWith(new RouteShareProperties(APP_SCHEME, null, LOGO_URL, IOS_STORE_URL, ANDROID_STORE_URL));
    }

    private MockMvc mockMvcWith(RouteShareProperties properties) {
        return MockMvcBuilders.standaloneSetup(new RouteShareLandingController(
                routeShareLandingService, new RouteShareLinkBuilder(properties), properties)).build();
    }

    private static Route sharedRoute(String title) {
        Route route = RouteFixture.createRoute(MEMBER_ID, title, START, START.plusDays(1), List.of(
                RouteFixture.createDay(1, START, 2),
                RouteFixture.createDay(2, START.plusDays(1), 1)
        ));
        route.share();
        route.assignShortCode("Ab12Cd34");
        return route;
    }

    @Test
    @DisplayName("공유 중인 루트는 OG 메타태그·일차별 스팟 목록·앱 딥링크·스토어 버튼을 담은 페이지를 응답한다")
    void landing_success() throws Exception {
        // given
        Route route = sharedRoute("Retro mood trip in Seongsu");
        given(routeShareLandingService.getLandingView(route.getPublicId()))
                .willReturn(RouteShareLandingView.from(route));

        // when
        MockHttpServletResponse response = mockMvc.perform(get("/routes/shared/{publicId}", route.getPublicId())
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "dev-api.moodi.kr")
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=300, public"))
                .andReturn().getResponse();
        String body = response.getContentAsString();

        // then
        assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
        assertThat(body)
                .contains("og:title\" content=\"Retro mood trip in Seongsu\"")
                .contains("og:description\" content=\"Sep 23 – Sep 24 · 2 days · 3 spots · 서울\"")
                .contains("og:image\" content=\"https://img.example.com/1.jpg\"")
                .contains("og:url\" content=\"https://dev-api.moodi.kr/s/Ab12Cd34\"")
                .contains("href=\"" + APP_SCHEME + route.getPublicId() + "\">Open in MOODI</a>")
                .contains("스팟 1").contains("스팟 2").contains("Day 2")
                .contains(IOS_STORE_URL)
                .contains(ANDROID_STORE_URL);
    }

    @Test
    @DisplayName("단축 링크 /s/{code} 도 같은 페이지를 그린다")
    void short_link_renders_same_page() throws Exception {
        // given
        Route route = sharedRoute("Retro mood trip in Seongsu");
        given(routeShareLandingService.getLandingViewByShortCode("Ab12Cd34"))
                .willReturn(RouteShareLandingView.from(route));

        // when
        String body = mockMvc.perform(get("/s/{code}", "Ab12Cd34")
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "dev-api.moodi.kr"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(body)
                .contains("og:title\" content=\"Retro mood trip in Seongsu\"")
                .contains("og:url\" content=\"https://dev-api.moodi.kr/s/Ab12Cd34\"")
                .contains(APP_SCHEME + route.getPublicId());
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 루트도 기본 제목으로 200 응답하고 캐시하지 않는다")
    void landing_not_found_falls_back() throws Exception {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeShareLandingService.getLandingView(publicId))
                .willReturn(RouteShareLandingView.notFound());

        // when
        String body = mockMvc.perform(get("/routes/shared/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(body)
                .contains("og:title\" content=\"Moodi\"")
                .contains(RouteShareLandingPage.NOT_FOUND_HEADLINE)
                .doesNotContain(APP_SCHEME);
    }

    @Test
    @DisplayName("존재하지 않는 단축 코드도 기본 제목으로 200 응답한다")
    void short_link_not_found_falls_back() throws Exception {
        // given
        given(routeShareLandingService.getLandingViewByShortCode("zzzzzzzz"))
                .willReturn(RouteShareLandingView.notFound());

        // when
        String body = mockMvc.perform(get("/s/{code}", "zzzzzzzz"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(body).contains("og:title\" content=\"Moodi\"");
    }

    @Test
    @DisplayName("잘못된 형식의 publicId도 예외 없이 기본 제목으로 200 응답한다")
    void landing_invalid_public_id_falls_back() throws Exception {
        // when
        String body = mockMvc.perform(get("/routes/shared/{publicId}", "not-a-uuid"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(body).contains("og:title\" content=\"Moodi\"");
    }

    @Test
    @DisplayName("루트 제목에 포함된 마크업은 이스케이프되어 그대로 실행되지 않는다")
    void landing_escapes_title() throws Exception {
        // given
        Route route = sharedRoute("<script>alert(1)</script>");
        given(routeShareLandingService.getLandingView(route.getPublicId()))
                .willReturn(RouteShareLandingView.from(route));

        // when
        String body = mockMvc.perform(get("/routes/shared/{publicId}", route.getPublicId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(body).doesNotContain("<script>alert(1)</script>");
        assertThat(body).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("스팟 이미지가 하나도 없으면 og:image 를 로고로 채운다")
    void landing_uses_logo_when_route_has_no_image() throws Exception {
        // given
        UUID publicId = UUID.randomUUID();
        RouteShareLandingView view = new RouteShareLandingView(true, publicId, null, "No photo route",
                START, START, 1, 0, null, null, List.of());
        given(routeShareLandingService.getLandingView(publicId)).willReturn(view);

        // when
        String body = mockMvc.perform(get("/routes/shared/{publicId}", publicId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(body).contains("og:image\" content=\"" + LOGO_URL + "\"");
    }

    @Test
    @DisplayName("로고 URL 미설정 시 요청 호스트 기준으로 og:image를 채운다")
    void landing_falls_back_to_request_host_for_logo_when_not_configured() throws Exception {
        // given
        MockMvc mockMvcWithoutLogo = mockMvcWith(
                new RouteShareProperties(APP_SCHEME, null, null, IOS_STORE_URL, ANDROID_STORE_URL));

        UUID publicId = UUID.randomUUID();
        RouteShareLandingView view = new RouteShareLandingView(true, publicId, null, "Dev route",
                START, START, 1, 0, null, null, List.of());
        given(routeShareLandingService.getLandingView(publicId)).willReturn(view);

        // when
        String body = mockMvcWithoutLogo.perform(get("/routes/shared/{publicId}", publicId)
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "dev-api.moodi.kr"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(body)
                .contains("og:image\" content=\"https://dev-api.moodi.kr/icon_ios.jpg\"")
                // 단축 코드가 없는 예전 공유 루트는 og:url 이 긴 링크다
                .contains("og:url\" content=\"https://dev-api.moodi.kr/routes/shared/" + publicId + "\"");
    }

    @Test
    @DisplayName("스토어 링크가 비어 있으면 스토어 버튼을 그리지 않는다")
    void landing_hides_store_buttons_when_not_configured() throws Exception {
        // given
        MockMvc mockMvcWithoutStores = mockMvcWith(new RouteShareProperties(APP_SCHEME, null, LOGO_URL, "", null));
        Route route = sharedRoute("Store-less route");
        given(routeShareLandingService.getLandingView(route.getPublicId()))
                .willReturn(RouteShareLandingView.from(route));

        // when
        String body = mockMvcWithoutStores.perform(get("/routes/shared/{publicId}", route.getPublicId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(body)
                .contains("Open in MOODI")
                .doesNotContain("App Store")
                .doesNotContain("Google Play");
    }
}
