package com.moodi.support.presentation;

import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.support.RestDocsSupport;
import com.moodi.support.application.NoticeQueryService;
import com.moodi.support.application.dto.NoticeDetail;
import com.moodi.support.application.dto.NoticeSummary;
import com.moodi.support.domain.NoticeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoticeControllerDocsTest extends RestDocsSupport {

    private final NoticeQueryService noticeQueryService = mock(NoticeQueryService.class);

    @Override
    protected Object initController() {
        return new NoticeController(noticeQueryService);
    }

    @Test
    @DisplayName("공지사항 목록 조회 성공")
    void get_notices_success() throws Exception {
        when(noticeQueryService.getVisibleNotices(isNull(), anyInt())).thenReturn(CursorResponse.of(
                List.of(
                        new NoticeSummary(12L, NoticeType.ANNOUNCEMENT, "Route sharing is now available",
                                "You can now share your routes with friends. Open a route and tap Share.",
                                LocalDate.of(2026, 8, 10)),
                        new NoticeSummary(11L, NoticeType.MAINTENANCE, "Scheduled maintenance on Aug 5",
                                "Moodi will be unavailable from 2:00 to 4:00 AM KST.",
                                LocalDate.of(2026, 8, 3))),
                "2026-08-03,11", true));

        mockMvc.perform(get("/api/v1/notices").param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("support/notice-list",
                        queryParameters(
                                parameterWithName("cursor").optional().description("커서 (이전 응답의 nextCursor)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 20)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("공지 목록"),
                                fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("공지 항목"),
                                fieldWithPath("data.items[].id").type(JsonFieldType.NUMBER).description("공지 ID"),
                                fieldWithPath("data.items[].type").type(JsonFieldType.STRING)
                                        .description("유형 (ANNOUNCEMENT, MAINTENANCE, UPDATE, ISSUE, EVENT, OTHER)"),
                                fieldWithPath("data.items[].title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.items[].preview").type(JsonFieldType.STRING)
                                        .description("본문 미리보기 (앞 200자, 줄바꿈 제거)"),
                                fieldWithPath("data.items[].publishedAt").type(JsonFieldType.STRING).description("등록일 (yyyy-MM-dd)"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).optional()
                                        .description("다음 페이지 커서 (마지막 페이지면 null)"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                ));
    }

    @Test
    @DisplayName("공지사항 상세 조회 성공")
    void get_notice_success() throws Exception {
        when(noticeQueryService.getVisibleNotice(12L)).thenReturn(new NoticeDetail(12L, NoticeType.ANNOUNCEMENT,
                "Route sharing is now available",
                "You can now share your routes with friends.\nOpen a route and tap Share.",
                true, LocalDate.of(2026, 8, 10)));

        mockMvc.perform(get("/api/v1/notices/{noticeId}", 12L))
                .andExpect(status().isOk())
                .andDo(document("support/notice-detail",
                        pathParameters(
                                parameterWithName("noticeId").description("공지 ID")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("공지 상세"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("공지 ID"),
                                fieldWithPath("data.type").type(JsonFieldType.STRING).description("유형"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("본문 전체 (줄바꿈 \\n 포함)"),
                                fieldWithPath("data.publishedAt").type(JsonFieldType.STRING).description("등록일 (yyyy-MM-dd)")
                        )
                ));
    }
}
