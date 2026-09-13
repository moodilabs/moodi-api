package com.moodi.admin.presentation;

import com.moodi.admin.application.AdminAuditService;
import com.moodi.admin.application.dto.AdminAuditLogItem;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.support.AdminRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

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

class AdminAuditLogControllerDocsTest extends AdminRestDocsSupport {

    private final AdminAuditService adminAuditService = mock(AdminAuditService.class);

    @Override
    protected Object initController() {
        return new AdminAuditLogController(adminAuditService);
    }

    @Test
    @DisplayName("[어드민] 감사 로그 조회 성공")
    void get_logs_success() throws Exception {
        when(adminAuditService.getLogs(isNull(), isNull())).thenReturn(CursorResponse.of(List.of(
                new AdminAuditLogItem(120L, adminId, "PATCH", "/api/admin/members/9c8b7a6d-5e4f-4321-8765-0fedcba98765/status",
                        204, "8f3a1c2e9b7d4f60", LocalDateTime.of(2026, 8, 10, 14, 2, 11)),
                new AdminAuditLogItem(119L, adminId, "POST", "/api/admin/notices", 201, "1d2c3b4a5f6e7089",
                        LocalDateTime.of(2026, 8, 10, 13, 40, 0))),
                null, false));

        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isOk())
                .andDo(document("admin/audit-logs",
                        queryParameters(
                                parameterWithName("adminId").optional().description("관리자 ID로 필터"),
                                parameterWithName("cursor").optional().description("이전 응답의 nextCursor (마지막 로그 ID)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("목록 (최신순, 100건씩)"),
                                fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("로그"),
                                fieldWithPath("data.items[].id").type(JsonFieldType.NUMBER).description("로그 ID"),
                                fieldWithPath("data.items[].adminId").type(JsonFieldType.STRING).description("관리자 ID"),
                                fieldWithPath("data.items[].method").type(JsonFieldType.STRING).description("HTTP 메서드"),
                                fieldWithPath("data.items[].path").type(JsonFieldType.STRING).description("요청 경로"),
                                fieldWithPath("data.items[].statusCode").type(JsonFieldType.NUMBER).description("응답 상태"),
                                fieldWithPath("data.items[].requestId").type(JsonFieldType.STRING).optional().description("X-Request-Id (앱 로그 대조용)"),
                                fieldWithPath("data.items[].createdAt").type(JsonFieldType.STRING).description("시각"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).optional().description("다음 커서"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 여부")
                        )
                ));
    }
}
