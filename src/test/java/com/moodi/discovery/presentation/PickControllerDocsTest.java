package com.moodi.discovery.presentation;

import com.moodi.discovery.application.ImageStorageClient;
import com.moodi.discovery.application.PickImageUploadService;
import com.moodi.discovery.application.PickResult;
import com.moodi.discovery.application.PickResultItem;
import com.moodi.discovery.application.PickService;
import com.moodi.discovery.domain.PickAreaLevel;
import com.moodi.discovery.presentation.dto.PickRequestDto;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.support.AuthenticatedRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PickControllerDocsTest extends AuthenticatedRestDocsSupport {

    private static final UUID PICK_ID = UUID.fromString("7c3a1f2e-0b64-4d19-9a52-5f2c8d3b1a90");

    private PickImageUploadService pickImageUploadService;
    private PickService pickService;

    @Override
    protected Object initController() {
        pickImageUploadService = mock(PickImageUploadService.class);
        pickService = mock(PickService.class);
        return new PickController(pickImageUploadService, pickService);
    }

    @Test
    @DisplayName("사진 업로드용 서명 URL 발급")
    void issue_upload_url() throws Exception {
        when(pickImageUploadService.issueUploadUrl(eq(memberId), anyString(), anyLong()))
                .thenReturn(new ImageStorageClient.UploadTarget(
                        "https://storage.googleapis.com/moodi-pick-uploads/picks/...?X-Goog-Signature=...",
                        "picks/3f1c.../8a2e....jpg",
                        300));

        mockMvc.perform(get("/api/v1/picks/upload-url")
                        .param("contentType", "image/jpeg")
                        .param("contentLength", "2097152"))
                .andExpect(status().isOk())
                .andDo(document("picks/upload-url",
                        queryParameters(
                                parameterWithName("contentType")
                                        .description("업로드할 사진의 MIME 타입. `image/jpeg` · `image/png` · `image/heic`만 허용"),
                                parameterWithName("contentLength")
                                        .description("업로드할 사진의 바이트 수. 최대 10485760 (10MB)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("업로드 대상"),
                                fieldWithPath("data.uploadUrl").type(JsonFieldType.STRING)
                                        .description("이 URL로 사진을 직접 PUT한다. 요청 시 `Content-Type` 헤더를 발급 때와 동일하게 보내야 한다"),
                                fieldWithPath("data.imageKey").type(JsonFieldType.STRING)
                                        .description("업로드 완료 후 추천 요청에 실어 보낼 객체 키. 비공개 버킷이라 그대로 열람할 수 없다"),
                                fieldWithPath("data.expiresInSeconds").type(JsonFieldType.NUMBER)
                                        .description("서명 URL 유효 시간(초). 만료되면 다시 발급받아야 한다")
                        )
                ));
    }

    @Test
    @DisplayName("지원하지 않는 형식이면 400을 응답한다")
    void issue_upload_url_unsupported_type() throws Exception {
        when(pickImageUploadService.issueUploadUrl(eq(memberId), anyString(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.PICK_UNSUPPORTED_IMAGE_TYPE));

        mockMvc.perform(get("/api/v1/picks/upload-url")
                        .param("contentType", "image/gif")
                        .param("contentLength", "1024"))
                .andExpect(status().isBadRequest())
                .andDo(document("picks/upload-url-unsupported-type"));
    }

    @Test
    @DisplayName("용량이 상한을 넘으면 400을 응답한다")
    void issue_upload_url_too_large() throws Exception {
        when(pickImageUploadService.issueUploadUrl(eq(memberId), anyString(), anyLong()))
                .thenThrow(new BusinessException(ErrorCode.PICK_IMAGE_TOO_LARGE));

        mockMvc.perform(get("/api/v1/picks/upload-url")
                        .param("contentType", "image/jpeg")
                        .param("contentLength", "20971520"))
                .andExpect(status().isBadRequest())
                .andDo(document("picks/upload-url-too-large"));
    }

    @Test
    @DisplayName("사진과 지역으로 스팟 추천")
    void recommend() throws Exception {
        when(pickService.recommend(eq(memberId), anyString(), anyList()))
                .thenReturn(new PickResult(PICK_ID, List.of(item(101L, "서울숲"), item(102L, "이바구길")), List.of()));

        mockMvc.perform(post("/api/v1/picks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PickRequestDto(
                                "picks/3f1c.../8a2e....jpg",
                                List.of(new PickRequestDto.AreaDto(PickAreaLevel.NEIGHBORHOOD, "서울", "성동구", "성수동"),
                                        new PickRequestDto.AreaDto(PickAreaLevel.REGION, "부산", null, null))))))
                .andExpect(status().isOk())
                .andDo(document("picks/recommend",
                        requestFields(
                                fieldWithPath("imageKey").type(JsonFieldType.STRING)
                                        .description("업로드 URL 발급 때 받은 객체 키"),
                                fieldWithPath("areas").type(JsonFieldType.ARRAY)
                                        .description("선택한 지역. 1개 이상 5개 이하"),
                                fieldWithPath("areas[].level").type(JsonFieldType.STRING)
                                        .description("`REGION`(시·도) · `DISTRICT`(시·군·구) · `NEIGHBORHOOD`(동·면)"),
                                fieldWithPath("areas[].region").type(JsonFieldType.STRING).description("시·도"),
                                fieldWithPath("areas[].district").type(JsonFieldType.STRING)
                                        .description("시·군·구. `DISTRICT` 이하에서 필수").optional(),
                                fieldWithPath("areas[].neighborhood").type(JsonFieldType.STRING)
                                        .description("동·면. `NEIGHBORHOOD`에서 필수").optional()
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("추천 결과"),
                                fieldWithPath("data.pickId").type(JsonFieldType.STRING)
                                        .description("추천 요청 ID"),
                                fieldWithPath("data.spots").type(JsonFieldType.ARRAY)
                                        .description("선택 지역 안의 추천 스팟. 최대 5개"),
                                fieldWithPath("data.spots[].spotId").type(JsonFieldType.NUMBER).description("스팟 ID"),
                                fieldWithPath("data.spots[].title").type(JsonFieldType.STRING).description("스팟명"),
                                fieldWithPath("data.spots[].imageUrl").type(JsonFieldType.STRING)
                                        .description("대표 이미지").optional(),
                                fieldWithPath("data.spots[].area").type(JsonFieldType.STRING).description("지역"),
                                fieldWithPath("data.spots[].address").type(JsonFieldType.STRING)
                                        .description("스팟 주소").optional(),
                                fieldWithPath("data.spots[].description").type(JsonFieldType.STRING)
                                        .description("스팟 설명").optional(),
                                fieldWithPath("data.spots[].latitude").type(JsonFieldType.NUMBER)
                                        .description("위도. 지도 마커 표시에 쓴다").optional(),
                                fieldWithPath("data.spots[].longitude").type(JsonFieldType.NUMBER)
                                        .description("경도").optional(),
                                fieldWithPath("data.spots[].moodTags").type(JsonFieldType.ARRAY)
                                        .description("무드 태그. 최대 3개"),
                                fieldWithPath("data.spots[].bookmarked").type(JsonFieldType.BOOLEAN)
                                        .description("현재 사용자 저장 여부"),
                                fieldWithPath("data.fallbackSpots").type(JsonFieldType.ARRAY)
                                        .description("`spots`가 비었을 때만 채워지는 대체 추천. 지역 조건을 풀고 뽑은 최대 5개")
                        )
                ));
    }

    @Test
    @DisplayName("추천 결과가 없으면 빈 배열과 대체 추천을 응답한다")
    void recommend_empty_result() throws Exception {
        when(pickService.recommend(eq(memberId), anyString(), anyList()))
                .thenReturn(new PickResult(PICK_ID, List.of(), List.of(item(201L, "감천문화마을"))));

        mockMvc.perform(post("/api/v1/picks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PickRequestDto("picks/a.jpg",
                                List.of(new PickRequestDto.AreaDto(PickAreaLevel.REGION, "서울", null, null))))))
                .andExpect(status().isOk())
                .andDo(document("picks/recommend-empty"));
    }

    @Test
    @DisplayName("지역을 하나도 고르지 않으면 400을 응답한다")
    void recommend_rejects_empty_areas() throws Exception {
        mockMvc.perform(post("/api/v1/picks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PickRequestDto("picks/a.jpg", List.of()))))
                .andExpect(status().isBadRequest())
                .andDo(document("picks/recommend-no-area"));
    }

    private PickResultItem item(long spotId, String title) {
        return new PickResultItem(spotId, title, "https://img.moodi.kr/spot" + spotId + ".jpg",
                "서울", "서울 성동구 성수동", "설명이 여기에 표시됩니다.", 37.5445, 127.0374,
                List.of(MoodTag.NATURE, MoodTag.SERENE, MoodTag.EXPANSIVE), false);
    }
}
