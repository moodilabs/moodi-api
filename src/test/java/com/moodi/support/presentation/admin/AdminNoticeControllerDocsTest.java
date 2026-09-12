package com.moodi.support.presentation.admin;

import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.support.AdminRestDocsSupport;
import com.moodi.support.application.NoticeAdminService;
import com.moodi.support.application.dto.NoticeCommand;
import com.moodi.support.application.dto.NoticeDetail;
import com.moodi.support.domain.NoticeType;
import com.moodi.support.presentation.dto.admin.AdminNoticeRequest;
import com.moodi.support.presentation.dto.admin.VisibilityRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminNoticeControllerDocsTest extends AdminRestDocsSupport {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final NoticeDetail DETAIL = new NoticeDetail(12L, NoticeType.ANNOUNCEMENT,
            "Route sharing is now available", "You can now share your routes.", true, LocalDate.of(2026, 8, 10));

    private final NoticeAdminService noticeAdminService = mock(NoticeAdminService.class);

    @Override
    protected Object initController() {
        return new AdminNoticeController(noticeAdminService, FIXED_CLOCK);
    }

    @Test
    @DisplayName("[어드민] 공지 목록 조회 성공")
    void get_notices_success() throws Exception {
        when(noticeAdminService.getNotices(isNull(), isNull(), isNull(), eq(20)))
                .thenReturn(CursorResponse.of(List.of(DETAIL), null, false));

        mockMvc.perform(get("/api/admin/notices").param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("admin/notices/list",
                        queryParameters(
                                parameterWithName("type").optional().description("유형 필터"),
                                parameterWithName("visible").optional().description("노출 여부 필터"),
                                parameterWithName("cursor").optional().description("커서"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 20)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("목록"),
                                fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("공지 (숨김 포함)"),
                                fieldWithPath("data.items[].id").type(JsonFieldType.NUMBER).description("ID"),
                                fieldWithPath("data.items[].type").type(JsonFieldType.STRING).description("유형"),
                                fieldWithPath("data.items[].title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.items[].content").type(JsonFieldType.STRING).description("본문"),
                                fieldWithPath("data.items[].visible").type(JsonFieldType.BOOLEAN).description("노출 여부"),
                                fieldWithPath("data.items[].publishedAt").type(JsonFieldType.STRING).description("등록일"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).optional().description("다음 커서"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 여부")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 공지 등록 성공")
    void create_notice_success() throws Exception {
        when(noticeAdminService.create(any(NoticeCommand.class))).thenReturn(12L);

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminNoticeRequest(NoticeType.ANNOUNCEMENT,
                                "Route sharing is now available", "You can now share your routes.", true, null))))
                .andExpect(status().isCreated())
                .andDo(document("admin/notices/create",
                        requestFields(
                                fieldWithPath("type").type(JsonFieldType.STRING)
                                        .description("유형 (ANNOUNCEMENT, MAINTENANCE, UPDATE, ISSUE, EVENT, OTHER)"),
                                fieldWithPath("title").type(JsonFieldType.STRING).description("제목 (≤100자)"),
                                fieldWithPath("content").type(JsonFieldType.STRING).description("본문 (≤10,000자)"),
                                fieldWithPath("visible").type(JsonFieldType.BOOLEAN).description("노출 여부"),
                                fieldWithPath("publishedAt").type(JsonFieldType.STRING).optional()
                                        .description("등록일 (yyyy-MM-dd, 미지정 시 오늘)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성 결과"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("생성된 공지 ID")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 공지 수정 성공")
    void update_notice_success() throws Exception {
        mockMvc.perform(put("/api/admin/notices/{noticeId}", 12L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminNoticeRequest(NoticeType.UPDATE,
                                "Transit times updated", "Updated body.", true, LocalDate.of(2026, 8, 3)))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/notices/update",
                        pathParameters(parameterWithName("noticeId").description("공지 ID"))
                ));

        verify(noticeAdminService).update(eq(12L), any(NoticeCommand.class));
    }

    @Test
    @DisplayName("[어드민] 공지 노출 전환 성공")
    void change_visibility_success() throws Exception {
        mockMvc.perform(patch("/api/admin/notices/{noticeId}/visibility", 12L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VisibilityRequest(false))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/notices/visibility",
                        pathParameters(parameterWithName("noticeId").description("공지 ID")),
                        requestFields(fieldWithPath("visible").type(JsonFieldType.BOOLEAN).description("노출 여부"))
                ));

        verify(noticeAdminService).changeVisibility(12L, false);
    }

    @Test
    @DisplayName("[어드민] 공지 삭제 성공")
    void delete_notice_success() throws Exception {
        mockMvc.perform(delete("/api/admin/notices/{noticeId}", 12L))
                .andExpect(status().isNoContent())
                .andDo(document("admin/notices/delete",
                        pathParameters(parameterWithName("noticeId").description("공지 ID"))
                ));

        verify(noticeAdminService).delete(12L);
    }
}
