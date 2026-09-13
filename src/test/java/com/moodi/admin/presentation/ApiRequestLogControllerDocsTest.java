package com.moodi.admin.presentation;

import com.moodi.admin.application.ApiRequestLogService;
import com.moodi.admin.application.dto.ApiRequestLogFilter;
import com.moodi.admin.application.dto.ApiRequestLogItem;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.support.AdminRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiRequestLogControllerDocsTest extends AdminRestDocsSupport {

    private final ApiRequestLogService apiRequestLogService = mock(ApiRequestLogService.class);

    @Override
    protected Object initController() {
        return new ApiRequestLogController(apiRequestLogService);
    }

    @Test
    @DisplayName("[어드민] 앱 API 요청 로그 조회 성공")
    void get_logs_success() throws Exception {
        UUID memberId = UUID.fromString("9c8b7a6d-5e4f-4321-8765-0fedcba98765");
        when(apiRequestLogService.getLogs(any(ApiRequestLogFilter.class), isNull(), eq(50)))
                .thenReturn(CursorResponse.of(List.of(
                        new ApiRequestLogItem(3021L, memberId, "jun", "jun@moodi.kr", "POST", "/api/v1/routes",
                                201, 842, "8f3a1c2e", LocalDateTime.of(2026, 9, 13, 18, 40, 5)),
                        new ApiRequestLogItem(3020L, null, null, null, "GET", "/api/v1/spots/15403",
                                200, 31, "1d2c3b4a", LocalDateTime.of(2026, 9, 13, 18, 39, 58))),
                        "3020", true));

        mockMvc.perform(get("/api/admin/api-logs").queryParam("statusClass", "2").queryParam("size", "50"))
                .andExpect(status().isOk())
                .andDo(document("admin/api-logs",
                        queryParameters(
                                parameterWithName("memberId").optional().description("회원 ID로 필터"),
                                parameterWithName("method").optional().description("HTTP 메서드 (GET, POST, …)"),
                                parameterWithName("path").optional().description("경로 부분 일치"),
                                parameterWithName("statusClass").optional().description("응답 코드 백의 자리 (2, 4, 5)"),
                                parameterWithName("cursor").optional().description("이전 응답의 nextCursor (마지막 로그 ID)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 50, 최대 200)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("목록 (최신순)"),
                                fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("로그"),
                                fieldWithPath("data.items[].id").type(JsonFieldType.NUMBER).description("로그 ID"),
                                fieldWithPath("data.items[].memberId").type(JsonFieldType.STRING).optional().description("회원 ID (비회원·인증 실패는 null)"),
                                fieldWithPath("data.items[].memberNickname").type(JsonFieldType.STRING).optional().description("회원 닉네임 (조회 시점, 탈퇴 시 null)"),
                                fieldWithPath("data.items[].memberEmail").type(JsonFieldType.STRING).optional().description("회원 이메일 (조회 시점, 탈퇴 시 null)"),
                                fieldWithPath("data.items[].method").type(JsonFieldType.STRING).description("HTTP 메서드"),
                                fieldWithPath("data.items[].path").type(JsonFieldType.STRING).description("요청 경로 (쿼리스트링 제외)"),
                                fieldWithPath("data.items[].statusCode").type(JsonFieldType.NUMBER).description("응답 상태"),
                                fieldWithPath("data.items[].durationMs").type(JsonFieldType.NUMBER).description("처리 시간 (ms)"),
                                fieldWithPath("data.items[].requestId").type(JsonFieldType.STRING).optional().description("X-Request-Id (앱 로그 대조용)"),
                                fieldWithPath("data.items[].createdAt").type(JsonFieldType.STRING).description("시각"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).optional().description("다음 커서"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 여부")
                        )
                ));
    }
}
