package com.moodi.member.presentation.admin;

import com.moodi.member.application.MemberAdminService;
import com.moodi.member.application.dto.MemberAdminDetail;
import com.moodi.member.application.dto.MemberAdminFilter;
import com.moodi.member.application.dto.MemberAdminRow;
import com.moodi.member.application.dto.MemberAdminStatus;
import com.moodi.member.application.dto.MemberDailyStat;
import com.moodi.member.domain.AgreementType;
import com.moodi.member.domain.Gender;
import com.moodi.member.domain.MemberStatus;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.member.presentation.dto.admin.AdminMemberStatusRequest;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.support.AdminRestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminMemberControllerDocsTest extends AdminRestDocsSupport {

    private static final UUID MEMBER_ID = UUID.fromString("9c8b7a6d-5e4f-4321-8765-0fedcba98765");
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 1, 10, 15, 30);

    private final MemberAdminService memberAdminService = mock(MemberAdminService.class);

    @Override
    protected Object initController() {
        return new AdminMemberController(memberAdminService);
    }

    @Test
    @DisplayName("[어드민] 회원 목록 조회 성공")
    void get_members_success() throws Exception {
        when(memberAdminService.getMembers(any(MemberAdminFilter.class), isNull(), eq(20)))
                .thenReturn(CursorResponse.of(List.of(new MemberAdminRow(MEMBER_ID, OAuthProvider.GOOGLE,
                        "moi1234@naver.com", "moi", "US", MemberAdminStatus.ACTIVE, CREATED_AT, null, null)),
                        null, false));

        mockMvc.perform(get("/api/admin/members").param("keyword", "moi").param("status", "ACTIVE").param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("admin/members/list",
                        queryParameters(
                                parameterWithName("keyword").optional().description("닉네임·이메일 부분일치 (대소문자 무시)"),
                                parameterWithName("status").optional().description("PENDING, ACTIVE, SUSPENDED, WITHDRAWN"),
                                parameterWithName("provider").optional().description("GOOGLE, APPLE"),
                                parameterWithName("cursor").optional().description("커서"),
                                parameterWithName("size").optional().description("페이지 크기 (기본 20)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("목록 (가입일 최신순)"),
                                fieldWithPath("data.items").type(JsonFieldType.ARRAY).description("회원"),
                                fieldWithPath("data.items[].id").type(JsonFieldType.STRING).description("회원 ID"),
                                fieldWithPath("data.items[].provider").type(JsonFieldType.STRING).description("소셜 제공자"),
                                fieldWithPath("data.items[].email").type(JsonFieldType.STRING).optional().description("이메일 (탈퇴 시 null)"),
                                fieldWithPath("data.items[].nickname").type(JsonFieldType.STRING).optional().description("닉네임 (온보딩 전·탈퇴 시 null)"),
                                fieldWithPath("data.items[].country").type(JsonFieldType.STRING).optional().description("국가"),
                                fieldWithPath("data.items[].status").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("data.items[].createdAt").type(JsonFieldType.STRING).description("가입 시각"),
                                fieldWithPath("data.items[].deletedAt").type(JsonFieldType.STRING).optional().description("탈퇴 시각"),
                                fieldWithPath("data.items[].suspendedAt").type(JsonFieldType.STRING).optional().description("정지 시각"),
                                fieldWithPath("data.nextCursor").type(JsonFieldType.STRING).optional().description("다음 커서"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 여부")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 회원 상세 조회 성공")
    void get_member_success() throws Exception {
        when(memberAdminService.getMember(MEMBER_ID)).thenReturn(new MemberAdminDetail(MEMBER_ID, OAuthProvider.GOOGLE,
                "moi1234@naver.com", "moi", "US", 1996, Gender.FEMALE, MemberAdminStatus.ACTIVE, CREATED_AT, null, null,
                null, null, List.of(new MemberAdminDetail.Agreement(AgreementType.TERMS_OF_SERVICE, true, CREATED_AT),
                new MemberAdminDetail.Agreement(AgreementType.MARKETING, false, null)),
                List.of(MoodTag.RETRO, MoodTag.COZY, MoodTag.LOCAL), 36L, 6L, 2L));

        mockMvc.perform(get("/api/admin/members/{memberId}", MEMBER_ID))
                .andExpect(status().isOk())
                .andDo(document("admin/members/detail",
                        pathParameters(parameterWithName("memberId").description("회원 ID")),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("회원 상세"),
                                fieldWithPath("data.id").type(JsonFieldType.STRING).description("회원 ID"),
                                fieldWithPath("data.provider").type(JsonFieldType.STRING).description("소셜 제공자"),
                                fieldWithPath("data.email").type(JsonFieldType.STRING).optional().description("이메일"),
                                fieldWithPath("data.nickname").type(JsonFieldType.STRING).optional().description("닉네임"),
                                fieldWithPath("data.country").type(JsonFieldType.STRING).optional().description("국가"),
                                fieldWithPath("data.birthYear").type(JsonFieldType.NUMBER).optional().description("출생연도"),
                                fieldWithPath("data.gender").type(JsonFieldType.STRING).optional().description("성별"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("가입 시각"),
                                fieldWithPath("data.deletedAt").type(JsonFieldType.STRING).optional().description("탈퇴 시각"),
                                fieldWithPath("data.suspendedAt").type(JsonFieldType.STRING).optional().description("정지 시각"),
                                fieldWithPath("data.suspendReason").type(JsonFieldType.STRING).optional().description("정지 사유"),
                                fieldWithPath("data.withdrawal").type(JsonFieldType.OBJECT).optional()
                                        .description("최근 탈퇴 사유 (탈퇴 회원만). reasons[]·detail·withdrawnAt"),
                                fieldWithPath("data.agreements").type(JsonFieldType.ARRAY).description("약관 동의 (탈퇴 시 비어 있음)"),
                                fieldWithPath("data.agreements[].type").type(JsonFieldType.STRING).description("약관 종류"),
                                fieldWithPath("data.agreements[].agreed").type(JsonFieldType.BOOLEAN).description("동의 여부"),
                                fieldWithPath("data.agreements[].agreedAt").type(JsonFieldType.STRING).optional().description("동의 시각"),
                                fieldWithPath("data.preferredMoods").type(JsonFieldType.ARRAY).description("선호 무드"),
                                fieldWithPath("data.savedSpotCount").type(JsonFieldType.NUMBER).description("북마크 수"),
                                fieldWithPath("data.routeCount").type(JsonFieldType.NUMBER).description("루트 수 (삭제 제외)"),
                                fieldWithPath("data.inquiryCount").type(JsonFieldType.NUMBER).description("1:1 문의 수")
                        )
                ));
    }

    @Test
    @DisplayName("[어드민] 회원 정지 성공")
    void suspend_member_success() throws Exception {
        mockMvc.perform(patch("/api/admin/members/{memberId}/status", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminMemberStatusRequest(MemberStatus.SUSPENDED, "스팸 문의 반복"))))
                .andExpect(status().isNoContent())
                .andDo(document("admin/members/change-status",
                        pathParameters(parameterWithName("memberId").description("회원 ID")),
                        requestFields(
                                fieldWithPath("status").type(JsonFieldType.STRING).description("SUSPENDED(정지) 또는 ACTIVE(해제)"),
                                fieldWithPath("reason").type(JsonFieldType.STRING).optional().description("정지 사유 (SUSPENDED일 때 필수, ≤200자)")
                        )
                ));

        verify(memberAdminService).suspend(MEMBER_ID, "스팸 문의 반복");
    }

    @Test
    @DisplayName("[어드민] 회원 강제 탈퇴 성공")
    void withdraw_member_success() throws Exception {
        mockMvc.perform(post("/api/admin/members/{memberId}/withdrawal", MEMBER_ID))
                .andExpect(status().isNoContent())
                .andDo(document("admin/members/withdraw",
                        pathParameters(parameterWithName("memberId").description("회원 ID"))
                ));

        verify(memberAdminService).withdraw(MEMBER_ID);
    }

    @Test
    @DisplayName("[어드민] 일별 가입·탈퇴 통계 조회 성공")
    void get_daily_stats_success() throws Exception {
        when(memberAdminService.getDailyStats(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3))).thenReturn(List.of(
                new MemberDailyStat(LocalDate.of(2026, 8, 1), 12, 0),
                new MemberDailyStat(LocalDate.of(2026, 8, 3), 8, 1)));

        mockMvc.perform(get("/api/admin/members/stats").param("from", "2026-08-01").param("to", "2026-08-03"))
                .andExpect(status().isOk())
                .andDo(document("admin/members/stats",
                        queryParameters(
                                parameterWithName("from").description("시작일 (yyyy-MM-dd, 포함)"),
                                parameterWithName("to").description("종료일 (포함, 시작일부터 최대 92일)")
                        ),
                        responseFields(
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("일별 통계 (활동 없는 날은 생략)"),
                                fieldWithPath("data[].date").type(JsonFieldType.STRING).description("날짜"),
                                fieldWithPath("data[].signups").type(JsonFieldType.NUMBER).description("가입 수"),
                                fieldWithPath("data[].withdrawals").type(JsonFieldType.NUMBER).description("탈퇴 수")
                        )
                ));
    }
}
