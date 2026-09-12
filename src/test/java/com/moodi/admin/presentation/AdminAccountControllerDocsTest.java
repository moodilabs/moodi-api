package com.moodi.admin.presentation;

import com.moodi.admin.application.AdminAccountService;
import com.moodi.admin.application.dto.AdminAccountCommand;
import com.moodi.admin.application.dto.AdminAccountInfo;
import com.moodi.admin.domain.AdminAccountStatus;
import com.moodi.admin.presentation.dto.AdminAccountCreateRequest;
import com.moodi.admin.presentation.dto.AdminAccountRoleRequest;
import com.moodi.admin.presentation.dto.AdminAccountStatusRequest;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.support.AdminRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAccountControllerDocsTest extends AdminRestDocsSupport {

    private final AdminAccountService adminAccountService = mock(AdminAccountService.class);

    @Override
    protected Object initController() {
        return new AdminAccountController(adminAccountService);
    }

    @Test
    @DisplayName("관리자 계정 목록 조회 성공")
    void get_accounts_success() throws Exception {
        when(adminAccountService.getAll()).thenReturn(List.of(new AdminAccountInfo(UUID.randomUUID(), "root@moodi.kr",
                "bootstrap", AdminRole.SUPER, AdminAccountStatus.ACTIVE, LocalDateTime.of(2026, 8, 10, 9, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0))));

        mockMvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isOk())
                .andDo(document("admin/accounts/list",
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("관리자 계정 (생성순)"),
                                fieldWithPath("data[].id").type(JsonFieldType.STRING).description("관리자 ID"),
                                fieldWithPath("data[].email").type(JsonFieldType.STRING).description("이메일"),
                                fieldWithPath("data[].name").type(JsonFieldType.STRING).description("이름"),
                                fieldWithPath("data[].role").type(JsonFieldType.STRING).description("권한"),
                                fieldWithPath("data[].status").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("data[].lastLoginAt").type(JsonFieldType.STRING).optional().description("마지막 로그인"),
                                fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).optional().description("생성일")
                        )
                ));
    }

    @Test
    @DisplayName("관리자 계정 생성 성공")
    void create_account_success() throws Exception {
        UUID created = UUID.randomUUID();
        when(adminAccountService.create(any(AdminAccountCommand.class))).thenReturn(created);

        mockMvc.perform(post("/api/admin/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminAccountCreateRequest("ops@moodi.kr",
                                "strong-password", "운영자", AdminRole.OPERATOR))))
                .andExpect(status().isCreated())
                .andDo(document("admin/accounts/create",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 (중복 불가)"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("초기 비밀번호 (10자 이상)"),
                                fieldWithPath("name").type(JsonFieldType.STRING).description("이름"),
                                fieldWithPath("role").type(JsonFieldType.STRING).description("권한 (SUPER, OPERATOR)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("생성 결과"),
                                fieldWithPath("data.id").type(JsonFieldType.STRING).description("생성된 관리자 ID")
                        )
                ));
    }

    @Test
    @DisplayName("관리자 계정 상태 변경 성공")
    void change_status_success() throws Exception {
        UUID target = UUID.randomUUID();

        mockMvc.perform(patch("/api/admin/accounts/{accountId}/status", target)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminAccountStatusRequest(AdminAccountStatus.DISABLED))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/accounts/change-status",
                        pathParameters(parameterWithName("accountId").description("대상 관리자 ID (본인 불가)")),
                        requestFields(
                                fieldWithPath("status").type(JsonFieldType.STRING).description("ACTIVE 또는 DISABLED (비활성화 시 세션 종료)")
                        )
                ));

        verify(adminAccountService).changeStatus(adminId, target, AdminAccountStatus.DISABLED);
    }

    @Test
    @DisplayName("관리자 권한 변경 성공")
    void change_role_success() throws Exception {
        UUID target = UUID.randomUUID();

        mockMvc.perform(patch("/api/admin/accounts/{accountId}/role", target)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminAccountRoleRequest(AdminRole.SUPER))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/accounts/change-role",
                        pathParameters(parameterWithName("accountId").description("대상 관리자 ID (본인 불가)")),
                        requestFields(
                                fieldWithPath("role").type(JsonFieldType.STRING).description("SUPER 또는 OPERATOR (변경 시 재로그인 필요)")
                        )
                ));

        verify(adminAccountService).changeRole(adminId, target, AdminRole.SUPER);
    }
}
