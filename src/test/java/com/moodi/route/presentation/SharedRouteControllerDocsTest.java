package com.moodi.route.presentation;

import com.moodi.route.application.RouteQueryService;
import com.moodi.route.domain.Route;
import com.moodi.route.support.RouteFixture;
import com.moodi.shared.auth.OptionalAuthMember;
import com.moodi.shared.support.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SharedRouteControllerDocsTest extends RestDocsSupport {

    private final RouteQueryService routeQueryService = mock(RouteQueryService.class);
    private final UUID memberId = UUID.randomUUID();

    @Override
    protected Object initController() {
        return new SharedRouteController(routeQueryService);
    }

    @Override
    protected HandlerMethodArgumentResolver[] argumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new OptionalAuthMemberStub()};
    }

    @Test
    @DisplayName("공유 루트 상세 조회 API")
    void get_shared_route_detail() throws Exception {
        // given
        UUID publicId = UUID.randomUUID();
        Route route = RouteFixture.createRoute(
                memberId, "Retro mood trip in Seongsu",
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11),
                List.of(
                        RouteFixture.createDay(1, LocalDate.of(2026, 8, 10), 2),
                        RouteFixture.createDay(2, LocalDate.of(2026, 8, 11), 2)
                )
        );
        route.share();

        given(routeQueryService.getSharedDetail(any(UUID.class)))
                .willReturn(route);

        // when & then
        mockMvc.perform(get("/api/routes/shared/{publicId}", publicId))
                .andExpect(status().isOk())
                .andDo(document("route-shared-detail",
                        responseFields(
                                fieldWithPath("data.publicId").description("루트 공개 식별자"),
                                fieldWithPath("data.title").description("루트 제목"),
                                fieldWithPath("data.startDate").description("여행 시작일"),
                                fieldWithPath("data.endDate").description("여행 종료일"),
                                fieldWithPath("data.totalDays").description("총 여행 일수"),
                                fieldWithPath("data.isOwner").description("요청자가 소유자인지 여부"),
                                fieldWithPath("data.days[].dayNumber").description("일차 번호"),
                                fieldWithPath("data.days[].date").description("해당 일자"),
                                fieldWithPath("data.days[].spots[].spotId").description("스팟 ID"),
                                fieldWithPath("data.days[].spots[].sequence").description("방문 순서"),
                                fieldWithPath("data.days[].spots[].estimatedMinutes").description("예상 체류시간(분)"),
                                fieldWithPath("data.days[].spots[].spotTitle").description("스팟명"),
                                fieldWithPath("data.days[].spots[].spotImageUrl").description("대표 이미지 URL"),
                                fieldWithPath("data.days[].spots[].spotArea").description("시/도"),
                                fieldWithPath("data.days[].spots[].spotDistrict").description("구/군"),
                                fieldWithPath("data.days[].spots[].spotLatitude").description("위도"),
                                fieldWithPath("data.days[].spots[].spotLongitude").description("경도"),
                                fieldWithPath("data.days[].spots[].spotContentType").description("스팟 유형"),
                                fieldWithPath("data.days[].spots[].spotDescription").description("스팟 설명"),
                                fieldWithPath("data.days[].legs[].fromSequence").description("출발 스팟 순서"),
                                fieldWithPath("data.days[].legs[].toSequence").description("도착 스팟 순서"),
                                fieldWithPath("data.days[].legs[].travelMode").description("이동수단 (WALK/PUBLIC_TRANSIT)"),
                                fieldWithPath("data.days[].legs[].durationSeconds").description("이동시간(초)"),
                                fieldWithPath("data.days[].legs[].distanceMeters").description("이동거리(미터)"),
                                fieldWithPath("data.days[].legs[].landingUrl").description("카카오맵 경로 링크")
                        )
                ));
    }

    private class OptionalAuthMemberStub implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(OptionalAuthMember.class)
                    && parameter.getParameterType().equals(UUID.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return memberId;
        }
    }
}
