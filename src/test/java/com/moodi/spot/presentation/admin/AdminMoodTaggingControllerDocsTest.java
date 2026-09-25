package com.moodi.spot.presentation.admin;

import com.moodi.shared.support.AdminRestDocsSupport;
import com.moodi.spot.application.MoodTaggingAdminService;
import com.moodi.spot.domain.MoodTaggingStatus;
import com.moodi.spot.domain.Spot;
import com.moodi.spot.domain.SpotContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminMoodTaggingControllerDocsTest extends AdminRestDocsSupport {

    private final MoodTaggingAdminService moodTaggingAdminService = mock(MoodTaggingAdminService.class);

    @Override
    protected Object initController() {
        return new AdminMoodTaggingController(moodTaggingAdminService);
    }

    @Test
    @DisplayName("[어드민] 태깅 현황 요약 조회 성공")
    void get_summary_success() throws Exception {
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("PENDING", 120L);
        statusCounts.put("PROCESSING", 3L);
        statusCounts.put("RETRY_WAIT", 17L);
        statusCounts.put("COMPLETED", 11433L);
        statusCounts.put("FAILED", 6L);
        when(moodTaggingAdminService.getSummary()).thenReturn(statusCounts);

        mockMvc.perform(get("/api/admin/mood-tagging/summary"))
                .andExpect(status().isOk())
                .andDo(document("admin/mood-tagging/summary",
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("태깅 현황"),
                                fieldWithPath("data.statusCounts").type(JsonFieldType.OBJECT).description("상태별 건수"),
                                fieldWithPath("data.statusCounts.PENDING").type(JsonFieldType.NUMBER).description("미처리"),
                                fieldWithPath("data.statusCounts.PROCESSING").type(JsonFieldType.NUMBER).description("처리 중"),
                                fieldWithPath("data.statusCounts.RETRY_WAIT").type(JsonFieldType.NUMBER).description("재시도 대기"),
                                fieldWithPath("data.statusCounts.COMPLETED").type(JsonFieldType.NUMBER).description("완료"),
                                fieldWithPath("data.statusCounts.FAILED").type(JsonFieldType.NUMBER).description("실패"),
                                fieldWithPath("data.total").type(JsonFieldType.NUMBER).description("전체 건수")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 태깅 실패 목록 조회 성공")
    void get_failed_success() throws Exception {
        Spot failedSpot = createFailedSpot(542L, "content-542", "서울",
                4, "LLM 응답 오류: 축 'atmosphere'이 응답에 없습니다",
                LocalDateTime.of(2026, 9, 25, 10, 32));
        when(moodTaggingAdminService.getFailed()).thenReturn(List.of(failedSpot));

        mockMvc.perform(get("/api/admin/mood-tagging/failed"))
                .andExpect(status().isOk())
                .andDo(document("admin/mood-tagging/failed",
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("실패 스팟 목록"),
                                fieldWithPath("data[].spotId").type(JsonFieldType.NUMBER).description("스팟 ID"),
                                fieldWithPath("data[].contentId").type(JsonFieldType.STRING).description("TourAPI contentId"),
                                fieldWithPath("data[].area").type(JsonFieldType.STRING).description("시·도"),
                                fieldWithPath("data[].attemptCount").type(JsonFieldType.NUMBER).description("시도 횟수"),
                                fieldWithPath("data[].lastError").type(JsonFieldType.STRING).description("마지막 오류"),
                                fieldWithPath("data[].lastAttemptedAt").type(JsonFieldType.STRING).description("마지막 시도 시각")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 태깅 수동 재시도 성공")
    void retry_success() throws Exception {
        mockMvc.perform(post("/api/admin/mood-tagging/spots/{spotId}/retry", 542L))
                .andExpect(status().isNoContent())
                .andDo(document("admin/mood-tagging/retry",
                        pathParameters(parameterWithName("spotId").description("스팟 ID"))
                ));

        verify(moodTaggingAdminService).retrySpot(542L);
    }

    private Spot createFailedSpot(Long id, String contentId, String area,
                                  int attemptCount, String lastError, LocalDateTime lastAttemptedAt) {
        Spot spot = Spot.create(contentId, SpotContentType.TOURIST_ATTRACTION, area,
                "종로구", null, "kor_service", null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(spot, "id", id);
        ReflectionTestUtils.setField(spot, "moodTaggingStatus", MoodTaggingStatus.FAILED);
        ReflectionTestUtils.setField(spot, "moodTaggingAttemptCount", attemptCount);
        ReflectionTestUtils.setField(spot, "moodTaggingLastError", lastError);
        ReflectionTestUtils.setField(spot, "moodTaggingLastAttemptedAt", lastAttemptedAt);
        return spot;
    }
}
