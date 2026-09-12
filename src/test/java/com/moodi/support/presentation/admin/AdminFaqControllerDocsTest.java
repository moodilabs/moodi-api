package com.moodi.support.presentation.admin;

import com.moodi.shared.support.AdminRestDocsSupport;
import com.moodi.support.application.FaqAdminService;
import com.moodi.support.application.dto.FaqCategoryCommand;
import com.moodi.support.application.dto.FaqCategoryView;
import com.moodi.support.application.dto.FaqCommand;
import com.moodi.support.application.dto.FaqItem;
import com.moodi.support.presentation.dto.admin.AdminFaqCategoryRequest;
import com.moodi.support.presentation.dto.admin.AdminFaqRequest;
import com.moodi.support.presentation.dto.admin.OrderRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminFaqControllerDocsTest extends AdminRestDocsSupport {

    private final FaqAdminService faqAdminService = mock(FaqAdminService.class);

    @Override
    protected Object initController() {
        return new AdminFaqController(faqAdminService);
    }

    @Test
    @DisplayName("[어드민] FAQ 전체 조회 성공")
    void get_all_success() throws Exception {
        when(faqAdminService.getAll()).thenReturn(List.of(new FaqCategoryView(1L, "Account", 0, true, List.of(
                new FaqItem(10L, "How do I change my username?", "Go to My > Account.", 0, true),
                new FaqItem(11L, "Hidden item", "Not shown in app.", 1, false)))));

        mockMvc.perform(get("/api/admin/faqs"))
                .andExpect(status().isOk())
                .andDo(document("admin/faqs/list",
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("유형 (순서대로, 숨김 포함)"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("유형 ID"),
                                fieldWithPath("data[].name").type(JsonFieldType.STRING).description("유형명"),
                                fieldWithPath("data[].sortOrder").type(JsonFieldType.NUMBER).description("유형 순서"),
                                fieldWithPath("data[].visible").type(JsonFieldType.BOOLEAN).description("유형 노출 여부"),
                                fieldWithPath("data[].items").type(JsonFieldType.ARRAY).description("항목 (순서대로, 숨김 포함)"),
                                fieldWithPath("data[].items[].id").type(JsonFieldType.NUMBER).description("FAQ ID"),
                                fieldWithPath("data[].items[].question").type(JsonFieldType.STRING).description("질문"),
                                fieldWithPath("data[].items[].answer").type(JsonFieldType.STRING).description("답변"),
                                fieldWithPath("data[].items[].sortOrder").type(JsonFieldType.NUMBER).description("항목 순서"),
                                fieldWithPath("data[].items[].visible").type(JsonFieldType.BOOLEAN).description("항목 노출 여부")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] FAQ 유형 등록 성공")
    void create_category_success() throws Exception {
        when(faqAdminService.createCategory(any(FaqCategoryCommand.class))).thenReturn(3L);

        mockMvc.perform(post("/api/admin/faq-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminFaqCategoryRequest("Routes", true))))
                .andExpect(status().isCreated())
                .andDo(document("admin/faqs/create-category",
                        requestFields(
                                fieldWithPath("name").type(JsonFieldType.STRING).description("유형명 (≤50자)"),
                                fieldWithPath("visible").type(JsonFieldType.BOOLEAN).description("노출 여부")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성 결과"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("생성된 유형 ID (마지막 순서)")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] FAQ 유형 순서 교체 성공")
    void reorder_categories_success() throws Exception {
        mockMvc.perform(put("/api/admin/faq-categories/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(List.of(3L, 1L, 2L)))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/faqs/reorder-categories",
                        requestFields(fieldWithPath("ids").type(JsonFieldType.ARRAY)
                                .description("전체 유형 ID를 원하는 순서로 (누락·중복 시 400)"))
                ));

        verify(faqAdminService).reorderCategories(List.of(3L, 1L, 2L));
    }

    @Test
    @DisplayName("[어드민] FAQ 유형 삭제 성공")
    void delete_category_success() throws Exception {
        mockMvc.perform(delete("/api/admin/faq-categories/{categoryId}", 3L))
                .andExpect(status().isNoContent())
                .andDo(document("admin/faqs/delete-category",
                        pathParameters(parameterWithName("categoryId").description("유형 ID (항목이 남아 있으면 409)"))
                ));

        verify(faqAdminService).deleteCategory(3L);
    }

    @Test
    @DisplayName("[어드민] FAQ 항목 등록 성공")
    void create_faq_success() throws Exception {
        when(faqAdminService.createFaq(any(FaqCommand.class))).thenReturn(20L);

        mockMvc.perform(post("/api/admin/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminFaqRequest(1L,
                                "How are travel times calculated?", "Moodi compares walking and transit.", true))))
                .andExpect(status().isCreated())
                .andDo(document("admin/faqs/create-faq",
                        requestFields(
                                fieldWithPath("categoryId").type(JsonFieldType.NUMBER).description("유형 ID"),
                                fieldWithPath("question").type(JsonFieldType.STRING).description("질문 (≤200자)"),
                                fieldWithPath("answer").type(JsonFieldType.STRING).description("답변 (≤10,000자)"),
                                fieldWithPath("visible").type(JsonFieldType.BOOLEAN).description("노출 여부")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성 결과"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("생성된 FAQ ID (유형 내 마지막 순서)")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] FAQ 항목 순서 교체 성공")
    void reorder_faqs_success() throws Exception {
        mockMvc.perform(put("/api/admin/faq-categories/{categoryId}/faqs/order", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(List.of(11L, 10L)))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/faqs/reorder-faqs",
                        pathParameters(parameterWithName("categoryId").description("유형 ID")),
                        requestFields(fieldWithPath("ids").type(JsonFieldType.ARRAY)
                                .description("해당 유형의 전체 FAQ ID를 원하는 순서로"))
                ));

        verify(faqAdminService).reorderFaqs(1L, List.of(11L, 10L));
    }
}
