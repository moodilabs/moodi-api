package com.moodi.route.presentation;

import com.moodi.route.application.RouteShareLandingService;
import com.moodi.route.application.RouteShareLandingView;
import com.moodi.route.application.RouteShareLinkBuilder;
import com.moodi.route.application.RouteShareProperties;
import com.moodi.route.domain.Route;
import com.moodi.route.support.RouteFixture;
import com.moodi.shared.support.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공유 랜딩 페이지는 REST API 가 아니라 HTML 이지만, 앱·카카오톡 카드가 링크하는 주소이므로 문서에 함께 싣는다.
 */
class RouteShareLandingControllerDocsTest extends RestDocsSupport {

    private static final String SHORT_CODE = "Ab12Cd34";
    private static final LocalDate START = LocalDate.of(2026, 9, 23);

    private final RouteShareLandingService routeShareLandingService = mock(RouteShareLandingService.class);
    private final UUID memberId = UUID.randomUUID();

    @Override
    protected Object initController() {
        RouteShareProperties properties = new RouteShareProperties(null, "https://dev-api.moodi.kr", null,
                "https://apps.apple.com/app/moodi", "https://play.google.com/store/apps/details?id=com.mudi");
        return new RouteShareLandingController(routeShareLandingService, new RouteShareLinkBuilder(properties), properties);
    }

    private Route sharedRoute() {
        Route route = RouteFixture.createRoute(memberId, "Retro mood trip in Seongsu", START, START.plusDays(1), List.of(
                RouteFixture.createDay(1, START, 2),
                RouteFixture.createDay(2, START.plusDays(1), 1)
        ));
        route.share();
        route.assignShortCode(SHORT_CODE);
        return route;
    }

    @Test
    @DisplayName("루트 공유 단축 링크 랜딩 페이지")
    void short_link_landing_page() throws Exception {
        // given
        given(routeShareLandingService.getLandingViewByShortCode(SHORT_CODE))
                .willReturn(RouteShareLandingView.from(sharedRoute()));

        // when & then
        mockMvc.perform(get("/s/{code}", SHORT_CODE))
                .andExpect(status().isOk())
                .andDo(document("route-share-short-link",
                        pathParameters(
                                parameterWithName("code").description("공유 활성화 응답의 shortCode (base62 8자리)")
                        )
                ));
    }

    @Test
    @DisplayName("루트 공유 랜딩 페이지 (publicId)")
    void landing_page_by_public_id() throws Exception {
        // given
        Route route = sharedRoute();
        given(routeShareLandingService.getLandingView(route.getPublicId()))
                .willReturn(RouteShareLandingView.from(route));

        // when & then
        mockMvc.perform(get("/routes/shared/{publicId}", route.getPublicId()))
                .andExpect(status().isOk())
                .andDo(document("route-share-landing",
                        pathParameters(
                                parameterWithName("publicId").description("루트 공개 식별자 (UUID)")
                        )
                ));
    }
}
