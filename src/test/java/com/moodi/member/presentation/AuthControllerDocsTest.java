package com.moodi.member.presentation;

import com.moodi.member.application.AuthService;
import com.moodi.member.application.dto.LoginResult;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.member.presentation.dto.LoginRequest;
import com.moodi.member.presentation.dto.ReissueRequest;
import com.moodi.shared.support.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerDocsTest extends RestDocsSupport {

    private final AuthService authService = mock(AuthService.class);

    @Override
    protected Object initController() {
        return new AuthController(authService);
    }

    @Test
    @DisplayName("소셜 로그인 성공")
    void social_login_success() throws Exception {
        when(authService.login(any(OAuthProvider.class), anyString()))
                .thenReturn(new LoginResult("access-token", "refresh-token", true));

        LoginRequest request = new LoginRequest("GOOGLE", "provider-id-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("auth/login",
                        requestFields(
                                fieldWithPath("provider").type(JsonFieldType.STRING).description("소셜 로그인 제공자 (GOOGLE, APPLE)"),
                                fieldWithPath("idToken").type(JsonFieldType.STRING).description("소셜 제공자가 발급한 id_token")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("토큰 정보"),
                                fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
                                fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
                                fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 타입 (Bearer)"),
                                fieldWithPath("data.isNewMember").type(JsonFieldType.BOOLEAN).description("신규 가입 여부")
                        )
                ));
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_success() throws Exception {
        when(authService.reissue(anyString()))
                .thenReturn(new LoginResult("new-access-token", "new-refresh-token", false));

        ReissueRequest request = new ReissueRequest("old-refresh-token");

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("auth/reissue",
                        requestFields(
                                fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("기존 리프레시 토큰")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("토큰 정보"),
                                fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
                                fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
                                fieldWithPath("data.tokenType").type(JsonFieldType.STRING).description("토큰 타입 (Bearer)"),
                                fieldWithPath("data.isNewMember").type(JsonFieldType.BOOLEAN).description("신규 가입 여부")
                        )
                ));
    }
}
