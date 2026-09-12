package com.moodi.admin.presentation;

import com.moodi.admin.application.AdminAccountService;
import com.moodi.admin.application.AdminAuthService;
import com.moodi.admin.application.dto.AdminAccountInfo;
import com.moodi.admin.application.dto.AdminLoginResult;
import com.moodi.admin.domain.AdminAccountStatus;
import com.moodi.admin.presentation.dto.AdminLoginRequest;
import com.moodi.admin.presentation.dto.AdminPasswordChangeRequest;
import com.moodi.admin.presentation.dto.AdminReissueRequest;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.support.AdminRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuthControllerDocsTest extends AdminRestDocsSupport {

    private final AdminAuthService adminAuthService = mock(AdminAuthService.class);
    private final AdminAccountService adminAccountService = mock(AdminAccountService.class);

    @Override
    protected Object initController() {
        return new AdminAuthController(adminAuthService, adminAccountService);
    }

    @Test
    @DisplayName("관리자 로그인 성공")
    void login_success() throws Exception {
        when(adminAuthService.login("ops@moodi.kr", "strong-password"))
                .thenReturn(new AdminLoginResult("admin-access-token", "admin-refresh-token", AdminRole.OPERATOR));

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminLoginRequest("ops@moodi.kr", "strong-password"))))
                .andExpect(status().isOk())
                .andDo(document("admin/auth/login",
                        requestFields(
                                fieldWithPath("email").type(JsonFieldType.STRING).description("관리자 이메일"),
                                fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("토큰"),
                                fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("관리자 액세스 토큰 (30분)"),
                                fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("관리자 리프레시 토큰 (12시간)"),
                                fieldWithPath("data.role").type(JsonFieldType.STRING).description("권한 (SUPER, OPERATOR)")
                        )
                ));
    }

    @Test
    @DisplayName("관리자 토큰 재발급 성공")
    void reissue_success() throws Exception {
        when(adminAuthService.reissue("admin-refresh-token"))
                .thenReturn(new AdminLoginResult("new-access", "new-refresh", AdminRole.OPERATOR));

        mockMvc.perform(post("/api/admin/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminReissueRequest("admin-refresh-token"))))
                .andExpect(status().isOk())
                .andDo(document("admin/auth/reissue",
                        requestFields(
                                fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("관리자 리프레시 토큰")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("토큰"),
                                fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("새 액세스 토큰"),
                                fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("새 리프레시 토큰 (이전 토큰은 폐기)"),
                                fieldWithPath("data.role").type(JsonFieldType.STRING).description("현재 권한 (DB 기준으로 갱신)")
                        )
                ));
    }

    @Test
    @DisplayName("관리자 로그아웃 성공")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/admin/auth/logout"))
                .andExpect(status().isNoContent())
                .andDo(document("admin/auth/logout"));

        verify(adminAuthService).logout(adminId);
    }

    @Test
    @DisplayName("내 관리자 정보 조회 성공")
    void get_me_success() throws Exception {
        when(adminAccountService.getMe(adminId)).thenReturn(new AdminAccountInfo(adminId, "ops@moodi.kr", "운영자",
                AdminRole.OPERATOR, AdminAccountStatus.ACTIVE, LocalDateTime.of(2026, 8, 10, 9, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0)));

        mockMvc.perform(get("/api/admin/me"))
                .andExpect(status().isOk())
                .andDo(document("admin/auth/me",
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("관리자 정보"),
                                fieldWithPath("data.id").type(JsonFieldType.STRING).description("관리자 ID"),
                                fieldWithPath("data.email").type(JsonFieldType.STRING).description("이메일"),
                                fieldWithPath("data.name").type(JsonFieldType.STRING).description("이름"),
                                fieldWithPath("data.role").type(JsonFieldType.STRING).description("권한"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("상태 (ACTIVE, DISABLED)"),
                                fieldWithPath("data.lastLoginAt").type(JsonFieldType.STRING).optional().description("마지막 로그인"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).optional().description("생성일")
                        )
                ));
    }

    @Test
    @DisplayName("관리자 비밀번호 변경 성공")
    void change_password_success() throws Exception {
        mockMvc.perform(patch("/api/admin/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminPasswordChangeRequest("old-password-1", "new-password-1"))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/auth/change-password",
                        requestFields(
                                fieldWithPath("currentPassword").type(JsonFieldType.STRING).description("현재 비밀번호"),
                                fieldWithPath("newPassword").type(JsonFieldType.STRING).description("새 비밀번호 (10자 이상)")
                        )
                ));

        verify(adminAccountService).changePassword(adminId, "old-password-1", "new-password-1");
    }
}
