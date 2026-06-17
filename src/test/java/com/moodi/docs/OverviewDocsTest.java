package com.moodi.docs;

import com.moodi.shared.docs.CommonDocumentation;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.PageResponse;
import com.moodi.shared.response.SuccessResponse;
import com.moodi.shared.support.RestDocsSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OverviewDocsTest extends RestDocsSupport {

    @Override
    protected Object initController() {
        return new OverviewTestController();
    }

    @Test
    void success_response() throws Exception {
        mockMvc.perform(get("/test/success"))
                .andExpect(status().isOk())
                .andDo(document("overview/success-response",
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                fieldWithPath("data.id").type(JsonFieldType.STRING).description("ID"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름")
                        )
                ));
    }

    @Test
    void page_response() throws Exception {
        mockMvc.perform(get("/test/page"))
                .andExpect(status().isOk())
                .andDo(document("overview/page-response",
                        responseFields(CommonDocumentation.pageResponseFields(
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("응답 데이터"),
                                fieldWithPath("data[].id").type(JsonFieldType.STRING).description("ID"),
                                fieldWithPath("data[].name").type(JsonFieldType.STRING).description("이름")
                        ))
                ));
    }

    @Test
    void business_exception() throws Exception {
        mockMvc.perform(get("/test/business-error"))
                .andExpect(status().isNotFound())
                .andDo(document("overview/business-exception",
                        responseFields(CommonDocumentation.errorResponseFields())
                ));
    }

    @Test
    void validation_error() throws Exception {
        mockMvc.perform(post("/test/validation-error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andDo(document("overview/validation-error",
                        responseFields(CommonDocumentation.errorResponseFields())
                ));
    }

    @Test
    void server_error() throws Exception {
        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andDo(document("overview/server-error",
                        responseFields(CommonDocumentation.errorResponseFields())
                ));
    }

    @RestController
    static class OverviewTestController {

        @GetMapping("/test/success")
        SuccessResponse<Object> success() {
            return SuccessResponse.of(Map.of("id", "1", "name", "테스트"));
        }

        @GetMapping("/test/page")
        PageResponse<Object> page() {
            return PageResponse.of(List.of(Map.of("id", "1", "name", "테스트")), 0, 10, 1L);
        }

        @GetMapping("/test/business-error")
        void businessError() {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @PostMapping("/test/validation-error")
        void validationError(@Valid @RequestBody ValidationRequest request) {}

        @GetMapping("/test/server-error")
        void serverError() {
            throw new RuntimeException("unexpected");
        }
    }

    record ValidationRequest(@NotBlank(message = "이름은 필수입니다") String name) {}
}
