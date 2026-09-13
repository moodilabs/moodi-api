package com.moodi.admin.presentation;

import com.moodi.admin.application.DashboardQueryService;
import com.moodi.admin.application.dto.DashboardSummary;
import com.moodi.shared.support.AdminRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerDocsTest extends AdminRestDocsSupport {

    private final DashboardQueryService dashboardQueryService = mock(DashboardQueryService.class);

    @Override
    protected Object initController() {
        return new DashboardController(dashboardQueryService);
    }

    @Test
    @DisplayName("[어드민] 대시보드 조회 성공")
    void get_dashboard_success() throws Exception {
        when(dashboardQueryService.getSummary()).thenReturn(new DashboardSummary(
                new DashboardSummary.Members(1200, 1100, 60, 3, 37, 12, 80),
                new DashboardSummary.Content(3400, 9800, 1200, 210, 560),
                new DashboardSummary.Inquiries(4, 11)));

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andDo(document("admin/dashboard",
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("집계"),
                                fieldWithPath("data.members").type(JsonFieldType.OBJECT).description("회원"),
                                fieldWithPath("data.members.total").type(JsonFieldType.NUMBER).description("전체 (탈퇴 포함)"),
                                fieldWithPath("data.members.active").type(JsonFieldType.NUMBER).description("가입 완료"),
                                fieldWithPath("data.members.pending").type(JsonFieldType.NUMBER).description("온보딩 중"),
                                fieldWithPath("data.members.suspended").type(JsonFieldType.NUMBER).description("정지"),
                                fieldWithPath("data.members.withdrawn").type(JsonFieldType.NUMBER).description("탈퇴"),
                                fieldWithPath("data.members.newToday").type(JsonFieldType.NUMBER).description("오늘 가입"),
                                fieldWithPath("data.members.newLast7Days").type(JsonFieldType.NUMBER).description("최근 7일 가입 (오늘 포함)"),
                                fieldWithPath("data.content").type(JsonFieldType.OBJECT).description("콘텐츠"),
                                fieldWithPath("data.content.spots").type(JsonFieldType.NUMBER).description("노출 중인 스팟 (PUBLISHED)"),
                                fieldWithPath("data.content.bookmarks").type(JsonFieldType.NUMBER).description("북마크"),
                                fieldWithPath("data.content.routes").type(JsonFieldType.NUMBER).description("루트 (삭제 제외)"),
                                fieldWithPath("data.content.sharedRoutes").type(JsonFieldType.NUMBER).description("공유된 루트"),
                                fieldWithPath("data.content.picks").type(JsonFieldType.NUMBER).description("Pick 요청"),
                                fieldWithPath("data.inquiries").type(JsonFieldType.OBJECT).description("문의"),
                                fieldWithPath("data.inquiries.received").type(JsonFieldType.NUMBER).description("미답변"),
                                fieldWithPath("data.inquiries.answeredLast7Days").type(JsonFieldType.NUMBER).description("최근 7일 답변")
                        )
                ));
    }
}
