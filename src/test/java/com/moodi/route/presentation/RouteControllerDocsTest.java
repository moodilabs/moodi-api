package com.moodi.route.presentation;

import com.moodi.route.application.RouteDeleteService;
import com.moodi.route.application.RouteGenerateCommand;
import com.moodi.route.application.RouteGenerateResult;
import com.moodi.route.application.RouteGenerateResult.DayResult;
import com.moodi.route.application.RouteGenerateResult.LegResultItem;
import com.moodi.route.application.RouteGenerateResult.SpotResult;
import com.moodi.route.application.RouteGenerateService;
import com.moodi.route.application.RouteListRow;
import com.moodi.route.application.RouteQueryService;
import com.moodi.route.application.RouteSaveCommand;
import com.moodi.route.application.RouteSaveService;
import com.moodi.route.domain.Route;
import com.moodi.route.presentation.dto.RouteGenerateRequest;
import com.moodi.route.presentation.dto.RouteListResponse;
import com.moodi.route.presentation.dto.RouteSaveRequest;
import com.moodi.route.presentation.dto.RouteSaveRequest.DayRequest;
import com.moodi.route.support.RouteFixture;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.support.AuthenticatedRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RouteControllerDocsTest extends AuthenticatedRestDocsSupport {

    private final RouteGenerateService routeGenerateService = mock(RouteGenerateService.class);
    private final RouteSaveService routeSaveService = mock(RouteSaveService.class);
    private final RouteQueryService routeQueryService = mock(RouteQueryService.class);
    private final RouteDeleteService routeDeleteService = mock(RouteDeleteService.class);

    @Override
    protected Object initController() {
        return new RouteController(routeGenerateService, routeSaveService, routeQueryService, routeDeleteService);
    }

    @Test
    @DisplayName("루트 초안 생성 API")
    void generate_route() throws Exception {
        // given
        RouteGenerateRequest request = new RouteGenerateRequest(
                List.of(1L, 2L, 3L, 4L), List.of("Seoul"),
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11));

        RouteGenerateResult result = new RouteGenerateResult(
                "Seoul 2-day course",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 11),
                List.of(
                        new DayResult(1, LocalDate.of(2026, 8, 10),
                                List.of(
                                        new SpotResult(1L, 1, 120, "Gyeongbokgung Palace", "https://img.example.com/1.jpg", "Seoul", "Jongno-gu", 37.5796, 126.9770, "TOURIST_ATTRACTION", "The main royal palace of the Joseon dynasty"),
                                        new SpotResult(2L, 2, 90, "MMCA Seoul", "https://img.example.com/2.jpg", "Seoul", "Jongno-gu", 37.5788, 126.9808, "CULTURAL_FACILITY", "National museum of modern and contemporary art")
                                ),
                                List.of(new LegResultItem(1, 2, "WALK", 480, 650, "https://map.kakao.com/link/to/Gyeongbokgung,37.5796,126.9770"))
                        ),
                        new DayResult(2, LocalDate.of(2026, 8, 11),
                                List.of(
                                        new SpotResult(3L, 1, 60, "Seongsu Cafe Street", "https://img.example.com/3.jpg", "Seoul", "Seongdong-gu", 37.5445, 127.0560, "TOURIST_ATTRACTION", "A trendy street full of hip cafes and galleries"),
                                        new SpotResult(4L, 2, 120, "Seoul Forest", "https://img.example.com/4.jpg", "Seoul", "Seongdong-gu", 37.5444, 127.0374, "TOURIST_ATTRACTION", "A large urban park to enjoy nature in the city")
                                ),
                                List.of(new LegResultItem(1, 2, "WALK", 900, 1200, "https://map.kakao.com/link/to/SeoulForest,37.5444,127.0374"))
                        )
                )
        );

        given(routeGenerateService.generate(any(RouteGenerateCommand.class))).willReturn(result);

        // when & then
        mockMvc.perform(post("/api/routes/generate")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("route-generate",
                        requestFields(
                                fieldWithPath("spotIds").description("기준 스팟 ID 목록 (1~10개, 중복 불가)"),
                                fieldWithPath("areas").description("여행 지역 (최대 5개, 선택)"),
                                fieldWithPath("startDate").description("여행 시작일"),
                                fieldWithPath("endDate").description("여행 종료일")
                        ),
                        responseFields(
                                fieldWithPath("data.title").description("AI 생성 루트 제목"),
                                fieldWithPath("data.startDate").description("여행 시작일"),
                                fieldWithPath("data.endDate").description("여행 종료일"),
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

    @Test
    @DisplayName("루트 저장 API")
    void save_route() throws Exception {
        // given
        RouteSaveRequest request = new RouteSaveRequest(
                "Retro mood trip in Seongsu",
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11),
                List.of(
                        new DayRequest(1, LocalDate.of(2026, 8, 10), List.of(1L, 2L)),
                        new DayRequest(2, LocalDate.of(2026, 8, 11), List.of(3L, 4L))
                )
        );

        Route route = RouteFixture.createRoute(
                memberId, "Retro mood trip in Seongsu",
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11),
                List.of(
                        RouteFixture.createDay(1, LocalDate.of(2026, 8, 10), 2),
                        RouteFixture.createDay(2, LocalDate.of(2026, 8, 11), 2)
                )
        );

        given(routeSaveService.save(any(RouteSaveCommand.class))).willReturn(route);

        // when & then
        mockMvc.perform(post("/api/routes")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("route-save",
                        requestFields(
                                fieldWithPath("title").description("루트 제목 (최대 40자)"),
                                fieldWithPath("startDate").description("여행 시작일"),
                                fieldWithPath("endDate").description("여행 종료일"),
                                fieldWithPath("days[].dayNumber").description("일차 번호"),
                                fieldWithPath("days[].date").description("해당 일자"),
                                fieldWithPath("days[].spotIds").description("스팟 ID 목록 (방문 순서)")
                        ),
                        saveResponseFields()
                ));
    }

    @Test
    @DisplayName("루트 수정 API")
    void update_route() throws Exception {
        // given
        UUID publicId = UUID.randomUUID();
        RouteSaveRequest request = new RouteSaveRequest(
                "Updated Seoul trip",
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11),
                List.of(
                        new DayRequest(1, LocalDate.of(2026, 8, 10), List.of(1L, 2L)),
                        new DayRequest(2, LocalDate.of(2026, 8, 11), List.of(3L, 4L))
                )
        );

        Route route = RouteFixture.createRoute(
                memberId, "Updated Seoul trip",
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11),
                List.of(
                        RouteFixture.createDay(1, LocalDate.of(2026, 8, 10), 2),
                        RouteFixture.createDay(2, LocalDate.of(2026, 8, 11), 2)
                )
        );

        given(routeSaveService.update(any(UUID.class), any(RouteSaveCommand.class))).willReturn(route);

        // when & then
        mockMvc.perform(put("/api/routes/{publicId}", publicId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("route-update",
                        requestFields(
                                fieldWithPath("title").description("루트 제목 (최대 40자)"),
                                fieldWithPath("startDate").description("여행 시작일"),
                                fieldWithPath("endDate").description("여행 종료일"),
                                fieldWithPath("days[].dayNumber").description("일차 번호"),
                                fieldWithPath("days[].date").description("해당 일자"),
                                fieldWithPath("days[].spotIds").description("스팟 ID 목록 (방문 순서)")
                        ),
                        saveResponseFields()
                ));
    }

    @Test
    @DisplayName("루트 목록 조회 API")
    void get_route_list() throws Exception {
        // given
        LocalDate start = LocalDate.of(2026, 8, 10);
        LocalDate end = LocalDate.of(2026, 8, 11);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 1, 14, 30, 0);

        CursorResponse<RouteListRow> result = CursorResponse.of(
                List.of(
                        new RouteListRow(6L, UUID.randomUUID(), "Retro mood trip in Seongsu", start, end, updatedAt),
                        new RouteListRow(5L, UUID.randomUUID(), "Busan coastal walk", start, end, updatedAt.minusHours(1))
                ),
                updatedAt.minusHours(1).toString() + ",5",
                true
        );

        given(routeQueryService.getList(any(UUID.class), isNull(), anyInt()))
                .willReturn(result);

        // when & then
        mockMvc.perform(get("/api/routes")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("route-list",
                        queryParameters(
                                parameterWithName("cursor").description("페이징 커서 (첫 페이지는 생략)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본 20)").optional()
                        ),
                        responseFields(
                                fieldWithPath("data.items[].publicId").description("루트 공개 식별자"),
                                fieldWithPath("data.items[].title").description("루트 제목"),
                                fieldWithPath("data.items[].startDate").description("여행 시작일"),
                                fieldWithPath("data.items[].endDate").description("여행 종료일"),
                                fieldWithPath("data.items[].totalDays").description("총 여행 일수"),
                                fieldWithPath("data.items[].updatedAt").description("최종 수정일시"),
                                fieldWithPath("data.nextCursor").description("다음 페이지 커서"),
                                fieldWithPath("data.hasNext").description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    @DisplayName("루트 상세 조회 API")
    void get_route_detail() throws Exception {
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

        given(routeQueryService.getDetail(any(UUID.class), any(UUID.class)))
                .willReturn(route);

        // when & then
        mockMvc.perform(get("/api/routes/{publicId}", publicId))
                .andExpect(status().isOk())
                .andDo(document("route-detail",
                        responseFields(
                                fieldWithPath("data.publicId").description("루트 공개 식별자"),
                                fieldWithPath("data.title").description("루트 제목"),
                                fieldWithPath("data.startDate").description("여행 시작일"),
                                fieldWithPath("data.endDate").description("여행 종료일"),
                                fieldWithPath("data.totalDays").description("총 여행 일수"),
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

    @Test
    @DisplayName("루트 삭제 API")
    void delete_route() throws Exception {
        // given
        UUID publicId = UUID.randomUUID();
        doNothing().when(routeDeleteService).delete(any(UUID.class), any(UUID.class));

        // when & then
        mockMvc.perform(delete("/api/routes/{publicId}", publicId))
                .andExpect(status().isNoContent())
                .andDo(document("route-delete"));
    }

    private org.springframework.restdocs.snippet.Snippet saveResponseFields() {
        return responseFields(
                fieldWithPath("data.publicId").description("루트 공개 식별자"),
                fieldWithPath("data.title").description("루트 제목"),
                fieldWithPath("data.startDate").description("여행 시작일"),
                fieldWithPath("data.endDate").description("여행 종료일"),
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
        );
    }
}
