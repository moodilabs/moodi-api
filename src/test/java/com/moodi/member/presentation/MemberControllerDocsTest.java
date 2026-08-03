package com.moodi.member.presentation;

import com.moodi.member.application.MemberOnboardingService;
import com.moodi.member.application.dto.AgreementCommand;
import com.moodi.member.application.dto.ProfileCommand;
import com.moodi.member.domain.Gender;
import com.moodi.member.presentation.dto.AgreementRequest;
import com.moodi.member.presentation.dto.ProfileRequest;
import com.moodi.shared.support.AuthenticatedRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerDocsTest extends AuthenticatedRestDocsSupport {

    private final MemberOnboardingService memberOnboardingService = mock(MemberOnboardingService.class);

    @Override
    protected Object initController() {
        return new MemberController(memberOnboardingService);
    }

    @Test
    @DisplayName("닉네임 중복 확인 성공")
    void nickname_availability_success() throws Exception {
        when(memberOnboardingService.isNicknameAvailable(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/v1/members/nickname-availability")
                        .param("nickname", "moodi_user"))
                .andExpect(status().isOk())
                .andDo(document("member/nickname-availability",
                        queryParameters(
                                parameterWithName("nickname").description("확인할 닉네임")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("확인 결과"),
                                fieldWithPath("data.available").type(JsonFieldType.BOOLEAN).description("사용 가능 여부")
                        )
                ));
    }

    @Test
    @DisplayName("프로필 설정 성공")
    void update_profile_success() throws Exception {
        ProfileRequest request = new ProfileRequest("moodi_user", "KR", 1996, Gender.FEMALE);

        mockMvc.perform(post("/api/v1/members/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andDo(document("member/profile",
                        requestFields(
                                fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임 (2~20자, 영문·숫자·'_'·'.')"),
                                fieldWithPath("country").type(JsonFieldType.STRING).description("국가 (ISO 3166-1 alpha-2)"),
                                fieldWithPath("birthYear").type(JsonFieldType.NUMBER).description("출생연도 (1900~현재연도, 만 14세 이상)"),
                                fieldWithPath("gender").type(JsonFieldType.STRING).description("성별 (MALE, FEMALE, OTHER)")
                        )
                ));

        verify(memberOnboardingService).updateProfile(any(), any(ProfileCommand.class));
    }

    @Test
    @DisplayName("약관 동의 및 가입 완료 성공")
    void agree_success() throws Exception {
        AgreementRequest request = new AgreementRequest(true, true, true, false);

        mockMvc.perform(post("/api/v1/members/agreements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andDo(document("member/agreements",
                        requestFields(
                                fieldWithPath("termsOfService").type(JsonFieldType.BOOLEAN).description("[필수] 이용약관 동의 여부"),
                                fieldWithPath("privacyPolicy").type(JsonFieldType.BOOLEAN).description("[필수] 개인정보 수집·이용 동의 여부"),
                                fieldWithPath("ageOver14").type(JsonFieldType.BOOLEAN).description("[필수] 만 14세 이상 확인 여부"),
                                fieldWithPath("marketing").type(JsonFieldType.BOOLEAN).description("[선택] 마케팅 수신 동의 여부")
                        )
                ));

        verify(memberOnboardingService).agree(any(), any(AgreementCommand.class));
    }
}
