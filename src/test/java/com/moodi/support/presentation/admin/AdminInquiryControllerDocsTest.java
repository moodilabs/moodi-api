package com.moodi.support.presentation.admin;

import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.support.AdminRestDocsSupport;
import com.moodi.support.application.InquiryAdminService;
import com.moodi.support.application.dto.AdminInquiryDetail;
import com.moodi.support.application.dto.AdminInquirySummary;
import com.moodi.support.application.dto.InquiryAttachmentView;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;
import com.moodi.support.presentation.dto.admin.AdminInquiryAnswerRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminInquiryControllerDocsTest extends AdminRestDocsSupport {

    private static final UUID INQUIRY_ID = UUID.fromString("3f1c2a9e-7b4d-4c8e-9f0a-1b2c3d4e5f60");
    private static final UUID MEMBER_ID = UUID.fromString("9c8b7a6d-5e4f-4321-8765-0fedcba98765");
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 3, 10, 15, 30);
    private static final AdminInquirySummary.Member MEMBER =
            new AdminInquirySummary.Member(MEMBER_ID, "moi", "moi1234@naver.com", false);

    private final InquiryAdminService inquiryAdminService = mock(InquiryAdminService.class);

    @Override
    protected Object initController() {
        return new AdminInquiryController(inquiryAdminService);
    }

    @Test
    @DisplayName("[어드민] 문의 목록 조회 성공")
    void get_inquiries_success() throws Exception {
        when(inquiryAdminService.getInquiries(eq(InquiryStatus.RECEIVED), isNull(), isNull(), eq(20)))
                .thenReturn(CursorResponse.of(List.of(new AdminInquirySummary(INQUIRY_ID, InquiryTopic.ROUTES,
                        "The travel time looks different from other map apps", InquiryStatus.RECEIVED, CREATED_AT,
                        null, MEMBER)), null, false));

        mockMvc.perform(get("/api/admin/inquiries").param("status", "RECEIVED").param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("admin/inquiries/list",
                        queryParameters(
                                parameterWithName("status").optional().description("상태 필터 (RECEIVED, ANSWERED)"),
                                parameterWithName("topic").optional().description("주제 필터"),
                                parameterWithName("cursor").optional().description("커서"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 20)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("목록 (최신순)"),
                                fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("문의"),
                                fieldWithPath("data.items[].id").type(JsonFieldType.STRING).description("문의 ID"),
                                fieldWithPath("data.items[].topic").type(JsonFieldType.STRING).description("주제"),
                                fieldWithPath("data.items[].subject").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.items[].status").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("data.items[].createdAt").type(JsonFieldType.STRING).description("등록 시각"),
                                fieldWithPath("data.items[].answeredAt").type(JsonFieldType.STRING).optional().description("답변 시각"),
                                fieldWithPath("data.items[].member").type(JsonFieldType.OBJECT).optional().description("문의 회원"),
                                fieldWithPath("data.items[].member.id").type(JsonFieldType.STRING).description("회원 ID"),
                                fieldWithPath("data.items[].member.nickname").type(JsonFieldType.STRING).optional().description("닉네임 (탈퇴 시 null)"),
                                fieldWithPath("data.items[].member.email").type(JsonFieldType.STRING).optional().description("이메일 (탈퇴 시 null)"),
                                fieldWithPath("data.items[].member.withdrawn").type(JsonFieldType.BOOLEAN).description("탈퇴 여부"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).optional().description("다음 커서"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 여부")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 미답변 문의 수 조회 성공")
    void count_received_success() throws Exception {
        when(inquiryAdminService.countReceived()).thenReturn(4L);

        mockMvc.perform(get("/api/admin/inquiries/count"))
                .andExpect(status().isOk())
                .andDo(document("admin/inquiries/count",
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과"),
                                fieldWithPath("data.count").type(JsonFieldType.NUMBER).description("RECEIVED 상태 문의 수")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 문의 상세 조회 성공")
    void get_inquiry_success() throws Exception {
        when(inquiryAdminService.get(INQUIRY_ID)).thenReturn(new AdminInquiryDetail(INQUIRY_ID, InquiryTopic.ROUTES,
                "The travel time looks different from other map apps",
                "I checked the route from Ikseon to Seongsu. Moodi says 18 minutes by transit, another app says 27.",
                InquiryStatus.ANSWERED, CREATED_AT,
                List.of(new InquiryAttachmentView("https://storage.googleapis.com/...signed-read", "image/jpeg")),
                MEMBER, new AdminInquiryDetail.Answer("Moodi shows the fastest option.", adminId, CREATED_AT.plusDays(1))));

        mockMvc.perform(get("/api/admin/inquiries/{inquiryId}", INQUIRY_ID))
                .andExpect(status().isOk())
                .andDo(document("admin/inquiries/detail",
                        pathParameters(parameterWithName("inquiryId").description("문의 ID")),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("문의 상세"),
                                fieldWithPath("data.id").type(JsonFieldType.STRING).description("문의 ID"),
                                fieldWithPath("data.topic").type(JsonFieldType.STRING).description("주제"),
                                fieldWithPath("data.subject").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("내용"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("등록 시각"),
                                fieldWithPath("data.attachments").type(JsonFieldType.ARRAY).description("첨부"),
                                fieldWithPath("data.attachments[].url").type(JsonFieldType.STRING).description("읽기용 서명 URL"),
                                fieldWithPath("data.attachments[].contentType").type(JsonFieldType.STRING).description("파일 형식"),
                                fieldWithPath("data.member").type(JsonFieldType.OBJECT).optional().description("문의 회원"),
                                fieldWithPath("data.member.id").type(JsonFieldType.STRING).description("회원 ID"),
                                fieldWithPath("data.member.nickname").type(JsonFieldType.STRING).optional().description("닉네임"),
                                fieldWithPath("data.member.email").type(JsonFieldType.STRING).optional().description("이메일"),
                                fieldWithPath("data.member.withdrawn").type(JsonFieldType.BOOLEAN).description("탈퇴 여부"),
                                fieldWithPath("data.answer").type(JsonFieldType.OBJECT).optional().description("답변"),
                                fieldWithPath("data.answer.content").type(JsonFieldType.STRING).description("답변 내용"),
                                fieldWithPath("data.answer.answeredBy").type(JsonFieldType.STRING).description("답변 관리자 ID"),
                                fieldWithPath("data.answer.answeredAt").type(JsonFieldType.STRING).description("답변 시각")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 문의 답변 등록 성공")
    void answer_success() throws Exception {
        mockMvc.perform(put("/api/admin/inquiries/{inquiryId}/answer", INQUIRY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminInquiryAnswerRequest("Moodi shows the fastest option."))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/inquiries/answer",
                        pathParameters(parameterWithName("inquiryId").description("문의 ID")),
                        requestFields(fieldWithPath("content").type(JsonFieldType.STRING).description("답변 내용 (≤5,000자). 재호출 시 덮어쓰기"))
                ));

        verify(inquiryAdminService).answer(INQUIRY_ID, adminId, "Moodi shows the fastest option.");
    }
}
