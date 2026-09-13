package com.moodi.spot.presentation.admin;

import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.support.AdminRestDocsSupport;
import com.moodi.spot.application.SpotAdminService;
import com.moodi.spot.application.dto.SpotAdminDetail;
import com.moodi.spot.application.dto.SpotAdminFilter;
import com.moodi.spot.application.dto.SpotAdminRow;
import com.moodi.spot.domain.SpotContentType;
import com.moodi.spot.domain.SpotStatus;
import com.moodi.spot.presentation.dto.admin.AdminSpotDescriptionRequest;
import com.moodi.spot.presentation.dto.admin.AdminSpotMoodRequest;
import com.moodi.spot.presentation.dto.admin.AdminSpotRouteExclusionRequest;
import com.moodi.spot.presentation.dto.admin.AdminSpotStatusRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminSpotControllerDocsTest extends AdminRestDocsSupport {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 7, 1, 3, 0);

    private final SpotAdminService spotAdminService = mock(SpotAdminService.class);

    @Override
    protected Object initController() {
        return new AdminSpotController(spotAdminService);
    }

    @Test
    @DisplayName("[어드민] 스팟 목록 조회 성공")
    void get_spots_success() throws Exception {
        when(spotAdminService.getSpots(any(SpotAdminFilter.class), isNull(), eq(20))).thenReturn(CursorResponse.of(
                List.of(new SpotAdminRow(1532L, "Daelim Changgo", SpotContentType.CULTURAL_FACILITY, "서울", "성동구",
                        SpotStatus.PUBLISHED, false, 1532L, CREATED_AT, null)), null, false));

        mockMvc.perform(get("/api/admin/spots").param("keyword", "changgo").param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("admin/spots/list",
                        queryParameters(
                                parameterWithName("keyword").optional().description("영문 제목·contentId 부분일치"),
                                parameterWithName("status").optional().description("TAGGING_PENDING, PUBLISHED, HIDDEN, DELETED"),
                                parameterWithName("area").optional().description("시·도 (예: 서울)"),
                                parameterWithName("cursor").optional().description("이전 응답의 nextCursor (마지막 스팟 ID)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 20)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("목록 (ID 내림차순)"),
                                fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("스팟"),
                                fieldWithPath("data.items[].id").type(JsonFieldType.NUMBER).description("스팟 ID"),
                                fieldWithPath("data.items[].title").type(JsonFieldType.STRING).optional().description("영문 제목 (번역 전이면 null)"),
                                fieldWithPath("data.items[].contentType").type(JsonFieldType.STRING).description("콘텐츠 유형"),
                                fieldWithPath("data.items[].area").type(JsonFieldType.STRING).description("시·도"),
                                fieldWithPath("data.items[].district").type(JsonFieldType.STRING).optional().description("구·군"),
                                fieldWithPath("data.items[].status").type(JsonFieldType.STRING).description("노출 상태"),
                                fieldWithPath("data.items[].routeExcluded").type(JsonFieldType.BOOLEAN).description("루트 생성 후보 제외 여부"),
                                fieldWithPath("data.items[].bookmarkCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.items[].createdAt").type(JsonFieldType.STRING).description("수집 시각"),
                                fieldWithPath("data.items[].statusChangedAt").type(JsonFieldType.STRING).optional().description("어드민 상태 변경 시각"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).optional().description("다음 커서"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 여부")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 스팟 상세 조회 성공")
    void get_spot_success() throws Exception {
        when(spotAdminService.getSpot(1532L)).thenReturn(new SpotAdminDetail(1532L, "2739870", "TOURAPI",
                SpotContentType.CULTURAL_FACILITY, "서울", "성동구", "성수동", 37.5446, 127.0558, "02-000-0000",
                "https://example.com", SpotStatus.PUBLISHED, null, null, false,
                new SpotAdminDetail.Translation("Daelim Changgo", "A 1970s rice warehouse turned gallery cafe.",
                        "78 Seongsui-ro, Seongdong-gu, Seoul", null),
                "A 1970s rice warehouse turned exhibition hall, where bare red brick and steel beams frame rotating art shows.",
                new SpotAdminDetail.Mood(List.of(MoodTag.RETRO, MoodTag.LIVELY, MoodTag.EXPANSIVE), 0.92),
                List.of(new SpotAdminDetail.Image("https://storage.googleapis.com/moodi-spot-images/1532/0.jpg", true, 0)),
                1532L, CREATED_AT, CREATED_AT));

        mockMvc.perform(get("/api/admin/spots/{spotId}", 1532L))
                .andExpect(status().isOk())
                .andDo(document("admin/spots/detail",
                        pathParameters(parameterWithName("spotId").description("스팟 ID")),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("스팟 상세"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("스팟 ID"),
                                fieldWithPath("data.contentId").type(JsonFieldType.STRING).description("TourAPI contentId"),
                                fieldWithPath("data.source").type(JsonFieldType.STRING).description("수집 출처"),
                                fieldWithPath("data.contentType").type(JsonFieldType.STRING).description("콘텐츠 유형"),
                                fieldWithPath("data.area").type(JsonFieldType.STRING).description("시·도"),
                                fieldWithPath("data.district").type(JsonFieldType.STRING).optional().description("구·군"),
                                fieldWithPath("data.neighborhood").type(JsonFieldType.STRING).optional().description("동"),
                                fieldWithPath("data.latitude").type(JsonFieldType.NUMBER).optional().description("위도"),
                                fieldWithPath("data.longitude").type(JsonFieldType.NUMBER).optional().description("경도"),
                                fieldWithPath("data.tel").type(JsonFieldType.STRING).optional().description("전화"),
                                fieldWithPath("data.homepage").type(JsonFieldType.STRING).optional().description("홈페이지"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("노출 상태"),
                                fieldWithPath("data.statusReason").type(JsonFieldType.STRING).optional().description("숨김·삭제 사유"),
                                fieldWithPath("data.statusChangedAt").type(JsonFieldType.STRING).optional().description("상태 변경 시각"),
                                fieldWithPath("data.routeExcluded").type(JsonFieldType.BOOLEAN).description("루트 생성 후보 제외"),
                                fieldWithPath("data.translation").type(JsonFieldType.OBJECT).optional().description("영문 번역"),
                                fieldWithPath("data.translation.title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.translation.overview").type(JsonFieldType.STRING).optional().description("TourAPI 개요"),
                                fieldWithPath("data.translation.addr1").type(JsonFieldType.STRING).optional().description("주소"),
                                fieldWithPath("data.translation.addr2").type(JsonFieldType.STRING).optional().description("상세 주소"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).optional().description("AI 설명 (en-US)"),
                                fieldWithPath("data.mood").type(JsonFieldType.OBJECT).optional().description("무드 (태깅 전이면 null)"),
                                fieldWithPath("data.mood.tags").type(JsonFieldType.ARRAY).description("무드 태그"),
                                fieldWithPath("data.mood.confidence").type(JsonFieldType.NUMBER).optional().description("태깅 신뢰도 (수동 보정 시 1.0)"),
                                fieldWithPath("data.images").type(JsonFieldType.ARRAY).description("이미지"),
                                fieldWithPath("data.images[].url").type(JsonFieldType.STRING).description("이미지 URL"),
                                fieldWithPath("data.images[].primary").type(JsonFieldType.BOOLEAN).description("대표 여부"),
                                fieldWithPath("data.images[].sortOrder").type(JsonFieldType.NUMBER).description("순서"),
                                fieldWithPath("data.bookmarkCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).optional().description("수집 시각"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).optional().description("수정 시각")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 스팟 숨김 성공")
    void change_status_success() throws Exception {
        mockMvc.perform(patch("/api/admin/spots/{spotId}/status", 1532L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminSpotStatusRequest(SpotStatus.HIDDEN, "폐업 확인 중"))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/spots/change-status",
                        pathParameters(parameterWithName("spotId").description("스팟 ID")),
                        requestFields(
                                fieldWithPath("status").type(JsonFieldType.STRING)
                                        .description("PUBLISHED(숨김 해제) · HIDDEN(숨김) · DELETED(삭제, 되돌릴 수 없음)"),
                                fieldWithPath("reason").type(JsonFieldType.STRING).optional()
                                        .description("사유 (HIDDEN·DELETED일 때 필수, ≤200자)")
                        )
                ));

        verify(spotAdminService).changeStatus(1532L, SpotStatus.HIDDEN, "폐업 확인 중");
    }

    @Test
    @DisplayName("[어드민] 루트 제외 전환 성공")
    void change_route_exclusion_success() throws Exception {
        mockMvc.perform(patch("/api/admin/spots/{spotId}/route-exclusion", 1532L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminSpotRouteExclusionRequest(true))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/spots/route-exclusion",
                        pathParameters(parameterWithName("spotId").description("스팟 ID")),
                        requestFields(fieldWithPath("excluded").type(JsonFieldType.BOOLEAN).description("true면 AI 루트 생성 후보에서 제외"))
                ));

        verify(spotAdminService).changeRouteExclusion(1532L, true);
    }

    @Test
    @DisplayName("[어드민] 무드 태그 보정 성공")
    void override_mood_tags_success() throws Exception {
        mockMvc.perform(put("/api/admin/spots/{spotId}/moods", 1532L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminSpotMoodRequest(List.of(MoodTag.RETRO, MoodTag.ARTSY)))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/spots/override-moods",
                        pathParameters(parameterWithName("spotId").description("스팟 ID")),
                        requestFields(fieldWithPath("moodTags").type(JsonFieldType.ARRAY)
                                .description("무드 태그 1개 이상 (MoodTag 20종, 전체 교체, 중복 제거)"))
                ));

        verify(spotAdminService).overrideMoodTags(1532L, List.of(MoodTag.RETRO, MoodTag.ARTSY));
    }

    @Test
    @DisplayName("[어드민] AI 설명 수정 성공")
    void update_description_success() throws Exception {
        mockMvc.perform(put("/api/admin/spots/{spotId}/description", 1532L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminSpotDescriptionRequest("A 1970s rice warehouse turned gallery cafe."))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/spots/update-description",
                        pathParameters(parameterWithName("spotId").description("스팟 ID")),
                        requestFields(fieldWithPath("content").type(JsonFieldType.STRING).description("영문 설명 (≤2,000자, 없으면 새로 생성)"))
                ));

        verify(spotAdminService).updateDescription(1532L, "A 1970s rice warehouse turned gallery cafe.");
    }
}
