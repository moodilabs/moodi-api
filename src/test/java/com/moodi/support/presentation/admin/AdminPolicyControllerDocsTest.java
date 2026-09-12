package com.moodi.support.presentation.admin;

import com.moodi.shared.support.AdminRestDocsSupport;
import com.moodi.support.application.PolicyAdminService;
import com.moodi.support.application.dto.PolicyCommand;
import com.moodi.support.application.dto.PolicyDetail;
import com.moodi.support.application.dto.PolicySummary;
import com.moodi.support.domain.PolicyType;
import com.moodi.support.presentation.dto.admin.AdminPolicyRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminPolicyControllerDocsTest extends AdminRestDocsSupport {

    private final PolicyAdminService policyAdminService = mock(PolicyAdminService.class);

    @Override
    protected Object initController() {
        return new AdminPolicyController(policyAdminService);
    }

    @Test
    @DisplayName("[어드민] 약관 버전 목록 조회 성공")
    void get_policies_success() throws Exception {
        when(policyAdminService.getAll(isNull())).thenReturn(List.of(
                new PolicySummary(3L, PolicyType.TERMS_OF_SERVICE, "2.0", LocalDate.of(2026, 9, 1)),
                new PolicySummary(1L, PolicyType.TERMS_OF_SERVICE, "1.2", LocalDate.of(2026, 8, 1))));

        mockMvc.perform(get("/api/admin/policies"))
                .andExpect(status().isOk())
                .andDo(document("admin/policies/list",
                        queryParameters(parameterWithName("type").optional().description("종류 필터")),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("버전 (미래 시행분 포함, 시행일 내림차순)"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("약관 ID"),
                                fieldWithPath("data[].type").type(JsonFieldType.STRING).description("종류"),
                                fieldWithPath("data[].version").type(JsonFieldType.STRING).description("버전"),
                                fieldWithPath("data[].effectiveAt").type(JsonFieldType.STRING).description("시행일")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 약관 상세 조회 성공")
    void get_policy_success() throws Exception {
        when(policyAdminService.get(3L)).thenReturn(new PolicyDetail(3L, PolicyType.TERMS_OF_SERVICE, "2.0",
                "Article 1 ...", LocalDate.of(2026, 9, 1)));

        mockMvc.perform(get("/api/admin/policies/{policyId}", 3L))
                .andExpect(status().isOk())
                .andDo(document("admin/policies/detail",
                        pathParameters(parameterWithName("policyId").description("약관 ID")),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("약관"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("약관 ID"),
                                fieldWithPath("data.type").type(JsonFieldType.STRING).description("종류"),
                                fieldWithPath("data.version").type(JsonFieldType.STRING).description("버전"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("전문"),
                                fieldWithPath("data.effectiveAt").type(JsonFieldType.STRING).description("시행일")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 약관 버전 등록 성공")
    void create_policy_success() throws Exception {
        when(policyAdminService.create(any(PolicyCommand.class))).thenReturn(3L);

        mockMvc.perform(post("/api/admin/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminPolicyRequest(PolicyType.TERMS_OF_SERVICE,
                                "2.0", "Article 1 ...", LocalDate.of(2026, 9, 1)))))
                .andExpect(status().isCreated())
                .andDo(document("admin/policies/create",
                        requestFields(
                                fieldWithPath("type").type(JsonFieldType.STRING).description("종류 (TERMS_OF_SERVICE, PRIVACY_POLICY)"),
                                fieldWithPath("version").type(JsonFieldType.STRING).description("버전 (종류 내 중복 불가, ≤20자)"),
                                fieldWithPath("content").type(JsonFieldType.STRING).description("전문"),
                                fieldWithPath("effectiveAt").type(JsonFieldType.STRING).description("시행일 (yyyy-MM-dd)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성 결과"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("생성된 약관 ID")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 약관 버전 삭제 성공")
    void delete_policy_success() throws Exception {
        mockMvc.perform(delete("/api/admin/policies/{policyId}", 3L))
                .andExpect(status().isNoContent())
                .andDo(document("admin/policies/delete",
                        pathParameters(parameterWithName("policyId").description("약관 ID (시행된 버전은 409)"))
                ));

        verify(policyAdminService).delete(3L);
    }
}
