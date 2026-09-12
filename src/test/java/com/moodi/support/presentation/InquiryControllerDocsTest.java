package com.moodi.support.presentation;

import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.support.AuthenticatedRestDocsSupport;
import com.moodi.support.application.InquiryAttachmentStorage;
import com.moodi.support.application.InquiryService;
import com.moodi.support.application.dto.InquiryAttachmentView;
import com.moodi.support.application.dto.InquiryCreateCommand;
import com.moodi.support.application.dto.InquiryDetail;
import com.moodi.support.application.dto.InquirySummary;
import com.moodi.support.domain.InquiryStatus;
import com.moodi.support.domain.InquiryTopic;
import com.moodi.support.presentation.dto.InquiryCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InquiryControllerDocsTest extends AuthenticatedRestDocsSupport {

    private static final UUID INQUIRY_ID = UUID.fromString("3f1c2a9e-7b4d-4c8e-9f0a-1b2c3d4e5f60");
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 3, 10, 15, 30);

    private final InquiryService inquiryService = mock(InquiryService.class);

    @Override
    protected Object initController() {
        return new InquiryController(inquiryService);
    }

    @Test
    @DisplayName("내 문의 목록 조회 성공")
    void get_inquiries_success() throws Exception {
        when(inquiryService.getMine(eq(memberId), isNull(), eq(20))).thenReturn(CursorResponse.of(List.of(
                new InquirySummary(INQUIRY_ID, InquiryTopic.ROUTES, "The travel time looks different from other map apps",
                        "I checked the route from Ikseon to Seongsu and Moodi says 18 minutes by transit.",
                        InquiryStatus.RECEIVED, CREATED_AT),
                new InquirySummary(UUID.randomUUID(), InquiryTopic.SPOT_INFORMATION, "A spot's opening hours are wrong",
                        "Daelim Changgo shows 11 AM but the actual opening is 12 PM.",
                        InquiryStatus.ANSWERED, CREATED_AT.minusDays(2))),
                null, false));

        mockMvc.perform(get("/api/v1/inquiries").param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("support/inquiry-list",
                        queryParameters(
                                parameterWithName("cursor").optional().description("커서 (이전 응답의 nextCursor)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 20)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("내 문의 목록 (최신순)"),
                                fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("문의"),
                                fieldWithPath("data.items[].id").type(JsonFieldType.STRING).description("문의 ID"),
                                fieldWithPath("data.items[].topic").type(JsonFieldType.STRING)
                                        .description("주제 (ACCOUNT, RECOMMENDATIONS, ROUTES, SPOT_INFORMATION, TECHNICAL_ISSUES, FEEDBACK_SUGGESTIONS, OTHER)"),
                                fieldWithPath("data.items[].subject").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.items[].preview").type(JsonFieldType.STRING).description("내용 미리보기 (앞 100자)"),
                                fieldWithPath("data.items[].status").type(JsonFieldType.STRING).description("상태 (RECEIVED: 접수, ANSWERED: 답변완료)"),
                                fieldWithPath("data.items[].createdAt").type(JsonFieldType.STRING).description("등록 시각"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).optional().description("다음 커서 (마지막이면 null)"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 여부")
                        )
                ));
    }

    @Test
    @DisplayName("문의 첨부 업로드 URL 발급 성공")
    void issue_upload_url_success() throws Exception {
        when(inquiryService.issueUploadUrl(memberId, "image/jpeg", 1048576L))
                .thenReturn(new InquiryAttachmentStorage.UploadTarget("https://storage.googleapis.com/moodi-inquiry-uploads/...signed",
                        "inquiries/" + memberId + "/8a2e4c1d-...jpg", 300));

        mockMvc.perform(get("/api/v1/inquiries/upload-url")
                        .param("contentType", "image/jpeg")
                        .param("contentLength", "1048576"))
                .andExpect(status().isOk())
                .andDo(document("support/inquiry-upload-url",
                        queryParameters(
                                parameterWithName("contentType").description("파일 형식 — image/jpeg, image/png, image/heic, video/mp4, video/quicktime, application/pdf"),
                                parameterWithName("contentLength").description("파일 크기(bytes). 사진·PDF 10MB, 동영상 50MB 이하")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("업로드 대상"),
                                fieldWithPath("data.uploadUrl").type(JsonFieldType.STRING).description("파일을 PUT할 서명 URL (Content-Type 헤더를 요청과 동일하게)"),
                                fieldWithPath("data.attachmentKey").type(JsonFieldType.STRING).description("업로드 후 문의 등록에 실을 키"),
                                fieldWithPath("data.expiresInSeconds").type(JsonFieldType.NUMBER).description("URL 유효 시간(초)")
                        )
                ));
    }

    @Test
    @DisplayName("문의 등록 성공")
    void create_inquiry_success() throws Exception {
        when(inquiryService.create(eq(memberId), any(InquiryCreateCommand.class))).thenReturn(INQUIRY_ID);
        InquiryCreateRequest request = new InquiryCreateRequest(InquiryTopic.ROUTES,
                "Can I add my own spot to a route?",
                "I want to add a small bakery near my hotel to my route, but I couldn't find it when I searched.",
                List.of(new InquiryCreateRequest.Attachment("inquiries/" + memberId + "/8a2e4c1d.jpg", "image/jpeg")));

        mockMvc.perform(post("/api/v1/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("support/inquiry-create",
                        requestFields(
                                fieldWithPath("topic").type(JsonFieldType.STRING).description("주제"),
                                fieldWithPath("subject").type(JsonFieldType.STRING).description("제목 (≤60자)"),
                                fieldWithPath("content").type(JsonFieldType.STRING).description("내용 (≤1,000자)"),
                                fieldWithPath("attachments").type(JsonFieldType.ARRAY).optional().description("첨부 (≤5개, 표시 순서대로)"),
                                fieldWithPath("attachments[].attachmentKey").type(JsonFieldType.STRING).description("업로드 URL 발급 때 받은 키 (본인 경로만 허용)"),
                                fieldWithPath("attachments[].contentType").type(JsonFieldType.STRING).description("파일 형식")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("등록 결과"),
                                fieldWithPath("data.id").type(JsonFieldType.STRING).description("문의 ID")
                        )
                ));
    }

    @Test
    @DisplayName("문의 상세 조회 성공")
    void get_inquiry_success() throws Exception {
        when(inquiryService.getMine(memberId, INQUIRY_ID)).thenReturn(new InquiryDetail(INQUIRY_ID,
                InquiryTopic.SPOT_INFORMATION, "A spot's opening hours are wrong",
                "Daelim Changgo shows 11 AM but the actual opening is 12 PM.", InquiryStatus.ANSWERED, CREATED_AT,
                List.of(new InquiryAttachmentView("https://storage.googleapis.com/...signed-read", "image/jpeg")),
                new InquiryDetail.Answer("Thanks for letting us know. We've updated the opening hours.",
                        CREATED_AT.plusDays(1))));

        mockMvc.perform(get("/api/v1/inquiries/{inquiryId}", INQUIRY_ID))
                .andExpect(status().isOk())
                .andDo(document("support/inquiry-detail",
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
                                fieldWithPath("data.attachments[].url").type(JsonFieldType.STRING).description("읽기용 서명 URL (5분)"),
                                fieldWithPath("data.attachments[].contentType").type(JsonFieldType.STRING).description("파일 형식"),
                                fieldWithPath("data.answer").type(JsonFieldType.OBJECT).optional().description("답변 (RECEIVED면 null)"),
                                fieldWithPath("data.answer.content").type(JsonFieldType.STRING).description("답변 내용"),
                                fieldWithPath("data.answer.answeredAt").type(JsonFieldType.STRING).description("답변 시각")
                        )
                ));
    }
}
