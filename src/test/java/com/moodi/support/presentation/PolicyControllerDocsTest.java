package com.moodi.support.presentation;

import com.moodi.shared.support.RestDocsSupport;
import com.moodi.support.application.PolicyQueryService;
import com.moodi.support.application.dto.PolicyDetail;
import com.moodi.support.application.dto.PolicySummary;
import com.moodi.support.domain.PolicyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolicyControllerDocsTest extends RestDocsSupport {

    private final PolicyQueryService policyQueryService = mock(PolicyQueryService.class);

    @Override
    protected Object initController() {
        return new PolicyController(policyQueryService);
    }

    @Test
    @DisplayName("약관 목록 조회 성공")
    void get_policies_success() throws Exception {
        when(policyQueryService.getCurrentPolicies()).thenReturn(List.of(
                new PolicySummary(1L, PolicyType.TERMS_OF_SERVICE, "1.2", LocalDate.of(2026, 8, 1)),
                new PolicySummary(2L, PolicyType.PRIVACY_POLICY, "1.0", LocalDate.of(2026, 7, 1))));

        mockMvc.perform(get("/api/v1/policies"))
                .andExpect(status().isOk())
                .andDo(document("support/policy-list",
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("약관 목록"),
                                fieldWithPath("data.policies").type(JsonFieldType.ARRAY).description("종류별 현재 적용 중인 약관"),
                                fieldWithPath("data.policies[].type").type(JsonFieldType.STRING)
                                        .description("종류 (TERMS_OF_SERVICE: 이용약관, PRIVACY_POLICY: 개인정보처리방침)"),
                                fieldWithPath("data.policies[].version").type(JsonFieldType.STRING).description("버전"),
                                fieldWithPath("data.policies[].locale").type(JsonFieldType.STRING).description("약관 언어 (ko-KR, en-US)"),
                                fieldWithPath("data.policies[].id").type(JsonFieldType.NUMBER).description("동의 요청에 전달할 약관 ID"),
                                fieldWithPath("data.policies[].effectiveAt").type(JsonFieldType.STRING).description("시행일 (yyyy-MM-dd)")
                        )
                ));
    }

    @Test
    @DisplayName("약관 상세 조회 성공")
    void get_policy_success() throws Exception {
        when(policyQueryService.getCurrentPolicy(PolicyType.TERMS_OF_SERVICE)).thenReturn(new PolicyDetail(1L,
                PolicyType.TERMS_OF_SERVICE, "1.2",
                "Article 1 (Purpose and Definitions)\n1. These Terms of Service set forth the basic terms ...",
                LocalDate.of(2026, 8, 1)));

        mockMvc.perform(get("/api/v1/policies/{type}", "TERMS_OF_SERVICE"))
                .andExpect(status().isOk())
                .andDo(document("support/policy-detail",
                        pathParameters(
                                parameterWithName("type").description("종류 (TERMS_OF_SERVICE, PRIVACY_POLICY)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("현재 적용 중인 약관"),
                                fieldWithPath("data.type").type(JsonFieldType.STRING).description("종류"),
                                fieldWithPath("data.version").type(JsonFieldType.STRING).description("버전"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("전문 (plain text, 줄바꿈 \\n 포함)"),
                                fieldWithPath("data.locale").type(JsonFieldType.STRING).description("약관 언어 (ko-KR, en-US)"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("동의 요청에 전달할 약관 ID"),
                                fieldWithPath("data.effectiveAt").type(JsonFieldType.STRING).description("시행일 (yyyy-MM-dd)")
                        )
                ));
    }
}
