package com.moodi.route.presentation;

import com.moodi.route.application.RouteShareLandingService;
import com.moodi.route.application.RouteShareLandingView;
import com.moodi.route.application.RouteShareProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RouteShareLandingControllerTest {

    @Mock
    private RouteShareLandingService routeShareLandingService;

    private MockMvc mockMvc;

    private static final String APP_SCHEME = "moodi://route/";
    private static final String LOGO_URL = "https://moodi.kr/logo.png";
    private static final String IOS_STORE_URL = "https://apps.apple.com/app/moodi";
    private static final String ANDROID_STORE_URL = "https://play.google.com/store/apps/details?id=com.moodi";

    @BeforeEach
    void setUp() {
        RouteShareProperties properties = new RouteShareProperties(APP_SCHEME, LOGO_URL, IOS_STORE_URL, ANDROID_STORE_URL);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new RouteShareLandingController(routeShareLandingService, properties)).build();
    }

    @Test
    @DisplayName("공유 중인 루트는 제목이 담긴 og:title 메타태그와 앱 딥링크를 응답한다")
    void landing_success() throws Exception {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeShareLandingService.getLandingView(publicId))
                .willReturn(RouteShareLandingView.of("Retro mood trip in Seongsu"));

        // when
        MockHttpServletResponse response = mockMvc.perform(get("/routes/shared/{publicId}", publicId)
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)"))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        String body = response.getContentAsString();

        // then
        assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
        assertThat(body).contains("og:title\" content=\"Retro mood trip in Seongsu\"");
        assertThat(body).contains("og:image\" content=\"" + LOGO_URL);
        assertThat(body).contains(APP_SCHEME + publicId);
        assertThat(body).contains(IOS_STORE_URL);
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 루트도 기본 제목으로 200 응답한다")
    void landing_not_found_falls_back() throws Exception {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeShareLandingService.getLandingView(publicId))
                .willReturn(RouteShareLandingView.notFound());

        // when
        String body = mockMvc.perform(get("/routes/shared/{publicId}", publicId))
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
        UUID publicId = UUID.randomUUID();
        given(routeShareLandingService.getLandingView(publicId))
                .willReturn(RouteShareLandingView.of("<script>alert(1)</script>"));

        // when
        String body = mockMvc.perform(get("/routes/shared/{publicId}", publicId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(body).doesNotContain("<script>alert(1)</script>");
        assertThat(body).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("로고 URL 미설정 시 요청 호스트 기준으로 og:image를 채운다")
    void landing_falls_back_to_request_host_for_logo_when_not_configured() throws Exception {
        // given
        RouteShareProperties properties = new RouteShareProperties(APP_SCHEME, null, IOS_STORE_URL, ANDROID_STORE_URL);
        MockMvc mockMvcWithoutLogo = MockMvcBuilders.standaloneSetup(
                new RouteShareLandingController(routeShareLandingService, properties)).build();

        UUID publicId = UUID.randomUUID();
        given(routeShareLandingService.getLandingView(publicId))
                .willReturn(RouteShareLandingView.of("Dev route"));

        // when
        String body = mockMvcWithoutLogo.perform(get("/routes/shared/{publicId}", publicId)
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "dev-api.moodi.kr"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(body).contains("og:image\" content=\"https://dev-api.moodi.kr/icon_ios.jpg\"");
    }

    @Test
    @DisplayName("데스크톱 User-Agent는 앱 딥링크로 리다이렉트하지 않는다")
    void landing_desktop_skips_redirect() throws Exception {
        // given
        UUID publicId = UUID.randomUUID();
        given(routeShareLandingService.getLandingView(publicId))
                .willReturn(RouteShareLandingView.of("Desktop route"));

        // when
        String body = mockMvc.perform(get("/routes/shared/{publicId}", publicId)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        assertThat(body).doesNotContain(APP_SCHEME);
    }
}
