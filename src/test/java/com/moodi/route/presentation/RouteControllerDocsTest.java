package com.moodi.route.presentation;

import com.moodi.route.application.RouteGenerateCommand;
import com.moodi.route.application.RouteGenerateResult;
import com.moodi.route.application.RouteGenerateResult.DayResult;
import com.moodi.route.application.RouteGenerateResult.LegResultItem;
import com.moodi.route.application.RouteGenerateResult.SpotResult;
import com.moodi.route.application.RouteGenerateService;
import com.moodi.route.presentation.dto.RouteGenerateRequest;
import com.moodi.shared.support.AuthenticatedRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RouteControllerDocsTest extends AuthenticatedRestDocsSupport {

    private final RouteGenerateService routeGenerateService = mock(RouteGenerateService.class);

    @Override
    protected Object initController() {
        return new RouteController(routeGenerateService);
    }

    @Test
    @DisplayName("루트 초안 생성 API")
    void generate_route() throws Exception {
        // given
        RouteGenerateRequest request = new RouteGenerateRequest(
                List.of(1L, 2L, 3L, 4L), List.of("서울"),
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11));

        RouteGenerateResult result = new RouteGenerateResult(
                "서울 1박 2일 코스",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 11),
                List.of(
                        new DayResult(1, LocalDate.of(2026, 8, 10),
                                List.of(
                                        new SpotResult(1L, 1, 120, "경복궁", "https://img.example.com/1.jpg", "서울", "종로구", 37.5796, 126.9770, "관광지", "조선 왕조의 법궁으로 웅장한 건축미를 자랑한다"),
                                        new SpotResult(2L, 2, 90, "국립현대미술관", "https://img.example.com/2.jpg", "서울", "종로구", 37.5788, 126.9808, "문화시설", "한국 근현대 미술 작품을 감상할 수 있는 미술관")
                                ),
                                List.of(new LegResultItem(1, 2, "WALK", 480, 650, "https://map.kakao.com/link/to/경복궁,37.5796,126.9770"))
                        ),
                        new DayResult(2, LocalDate.of(2026, 8, 11),
                                List.of(
                                        new SpotResult(3L, 1, 60, "성수동 카페거리", "https://img.example.com/3.jpg", "서울", "성동구", 37.5445, 127.0560, "관광지", "힙한 카페와 갤러리가 모여 있는 성수동 거리"),
                                        new SpotResult(4L, 2, 120, "서울숲", "https://img.example.com/4.jpg", "서울", "성동구", 37.5444, 127.0374, "관광지", "도심 속 자연을 즐길 수 있는 대형 공원")
                                ),
                                List.of(new LegResultItem(1, 2, "WALK", 900, 1200, "https://map.kakao.com/link/to/서울숲,37.5444,127.0374"))
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
}
