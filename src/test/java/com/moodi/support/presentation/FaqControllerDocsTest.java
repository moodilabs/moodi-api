package com.moodi.support.presentation;

import com.moodi.shared.support.RestDocsSupport;
import com.moodi.support.application.FaqQueryService;
import com.moodi.support.application.dto.FaqCategoryView;
import com.moodi.support.application.dto.FaqItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FaqControllerDocsTest extends RestDocsSupport {

    private final FaqQueryService faqQueryService = mock(FaqQueryService.class);

    @Override
    protected Object initController() {
        return new FaqController(faqQueryService);
    }

    @Test
    @DisplayName("FAQ 목록 조회 성공")
    void get_faqs_success() throws Exception {
        when(faqQueryService.getVisibleFaqs()).thenReturn(List.of(
                new FaqCategoryView(1L, "Account", 0, true, List.of(
                        new FaqItem(10L, "How do I change my username?",
                                "Go to My > Account > Username and enter a new one.", 0, true),
                        new FaqItem(11L, "Can I use Moodi without signing up?",
                                "Yes. You can browse spots as a guest, but saving spots and routes requires an account.",
                                1, true))),
                new FaqCategoryView(2L, "Travel times", 1, true, List.of(
                        new FaqItem(20L, "How are travel times calculated?",
                                "Moodi compares walking and public transit, then shows whichever is faster.", 0, true)))));

        mockMvc.perform(get("/api/v1/faqs"))
                .andExpect(status().isOk())
                .andDo(document("support/faq-list",
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("FAQ 목록"),
                                fieldWithPath("data.categories").type(JsonFieldType.ARRAY).description("질문 유형 (어드민 지정 순서)"),
                                fieldWithPath("data.categories[].id").type(JsonFieldType.NUMBER).description("유형 ID"),
                                fieldWithPath("data.categories[].name").type(JsonFieldType.STRING).description("유형명"),
                                fieldWithPath("data.categories[].items").type(JsonFieldType.ARRAY).description("유형 내 FAQ 항목 (어드민 지정 순서)"),
                                fieldWithPath("data.categories[].items[].id").type(JsonFieldType.NUMBER).description("FAQ ID"),
                                fieldWithPath("data.categories[].items[].question").type(JsonFieldType.STRING).description("질문"),
                                fieldWithPath("data.categories[].items[].answer").type(JsonFieldType.STRING).description("답변 (줄바꿈 \\n 포함)")
                        )
                ));
    }
}
