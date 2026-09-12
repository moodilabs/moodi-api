package com.moodi.member.application;

import com.moodi.member.application.dto.MemberAdminDetail;
import com.moodi.member.application.dto.MemberAdminFilter;
import com.moodi.member.application.dto.MemberAdminRow;
import com.moodi.member.application.dto.MemberAdminStatus;
import com.moodi.member.domain.AgreementType;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberAgreement;
import com.moodi.member.domain.MemberAgreementRepository;
import com.moodi.member.domain.MemberPreferredMood;
import com.moodi.member.domain.MemberPreferredMoodRepository;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.member.domain.RefreshTokenRepository;
import com.moodi.member.support.MemberFixture;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.mood.MoodTag;
import com.moodi.shared.response.CursorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAdminServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 1, 10, 0);

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberAdminQueryRepository memberAdminQueryRepository;
    @Mock
    private MemberAgreementRepository memberAgreementRepository;
    @Mock
    private MemberPreferredMoodRepository memberPreferredMoodRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private BookmarkCountReader bookmarkCountReader;
    @Mock
    private RouteCountReader routeCountReader;
    @Mock
    private InquiryCountReader inquiryCountReader;
    @Mock
    private MemberWithdrawService memberWithdrawService;

    private MemberAdminService memberAdminService;

    @BeforeEach
    void setUp() {
        memberAdminService = new MemberAdminService(memberRepository, memberAdminQueryRepository,
                memberAgreementRepository, memberPreferredMoodRepository, refreshTokenRepository,
                bookmarkCountReader, routeCountReader, inquiryCountReader, memberWithdrawService, FIXED_CLOCK);
    }

    @Test
    @DisplayName("회원 목록은 필터를 그대로 넘기고 커서 페이징한다")
    void get_members_paginates() {
        MemberAdminFilter filter = new MemberAdminFilter("moi", MemberAdminStatus.ACTIVE, OAuthProvider.GOOGLE);
        when(memberAdminQueryRepository.findAll(eq(filter), isNull(), isNull(), eq(2))).thenReturn(List.of(
                row(UUID.randomUUID(), CREATED_AT.plusDays(2)),
                row(MEMBER_ID, CREATED_AT.plusDays(1))));

        CursorResponse<MemberAdminRow> result = memberAdminService.getMembers(filter, null, 1);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).startsWith(CREATED_AT.plusDays(2).toString() + ",");
    }

    @Test
    @DisplayName("회원 상세는 약관·선호 무드·활동 수를 모은다")
    void get_member_aggregates_detail() {
        Member member = MemberFixture.createWithId(MEMBER_ID);
        member.updateProfile("moodi_user", "KR", 1996, com.moodi.member.domain.Gender.FEMALE, MemberFixture.CURRENT_YEAR);
        member.activate();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberAgreementRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(
                MemberAgreement.of(MEMBER_ID, AgreementType.TERMS_OF_SERVICE, true, CREATED_AT),
                MemberAgreement.of(MEMBER_ID, AgreementType.MARKETING, false, CREATED_AT)));
        when(memberPreferredMoodRepository.findByMemberId(MEMBER_ID)).thenReturn(List.of(
                MemberPreferredMood.of(MEMBER_ID, MoodTag.RETRO)));
        when(bookmarkCountReader.countByMemberId(MEMBER_ID)).thenReturn(36L);
        when(routeCountReader.countActiveByMemberId(MEMBER_ID)).thenReturn(6L);
        when(inquiryCountReader.countByMemberId(MEMBER_ID)).thenReturn(2L);

        MemberAdminDetail detail = memberAdminService.getMember(MEMBER_ID);

        assertThat(detail.status()).isEqualTo(MemberAdminStatus.ACTIVE);
        assertThat(detail.agreements()).hasSize(2);
        assertThat(detail.agreements().get(1).agreedAt()).isNull();
        assertThat(detail.preferredMoods()).containsExactly(MoodTag.RETRO);
        assertThat(detail.savedSpotCount()).isEqualTo(36L);
        assertThat(detail.inquiryCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("탈퇴 회원 상세는 WITHDRAWN으로 표시된다")
    void get_withdrawn_member_shows_withdrawn() {
        Member member = MemberFixture.createWithId(MEMBER_ID);
        member.withdraw(CREATED_AT);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        MemberAdminDetail detail = memberAdminService.getMember(MEMBER_ID);

        assertThat(detail.status()).isEqualTo(MemberAdminStatus.WITHDRAWN);
        assertThat(detail.deletedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("정지하면 상태가 바뀌고 리프레시 토큰이 지워진다")
    void suspend_changes_status_and_revokes_tokens() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        memberAdminService.suspend(MEMBER_ID, "스팸 문의 반복");

        assertThat(member.isSuspended()).isTrue();
        assertThat(member.getSuspendedAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
        verify(memberRepository).save(member);
        verify(refreshTokenRepository).deleteByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("탈퇴한 회원은 정지할 수 없다")
    void suspend_withdrawn_member_throws() {
        Member member = MemberFixture.active();
        member.withdraw(CREATED_AT);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberAdminService.suspend(MEMBER_ID, "reason"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        verify(refreshTokenRepository, never()).deleteByMemberId(any());
    }

    @Test
    @DisplayName("정지 해제하면 ACTIVE로 돌아간다")
    void unsuspend_restores_active() {
        Member member = MemberFixture.active();
        member.suspend("reason", CREATED_AT);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        memberAdminService.unsuspend(MEMBER_ID);

        assertThat(member.isSuspended()).isFalse();
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("강제 탈퇴는 회원 탈퇴 서비스에 위임한다")
    void withdraw_delegates() {
        memberAdminService.withdraw(MEMBER_ID);

        verify(memberWithdrawService).withdraw(MEMBER_ID);
    }

    @Test
    @DisplayName("통계 기간이 92일을 넘거나 역순이면 거부한다")
    void get_daily_stats_validates_range() {
        LocalDate from = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> memberAdminService.getDailyStats(from, from.minusDays(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThatThrownBy(() -> memberAdminService.getDailyStats(from, from.plusDays(93)))
                .isInstanceOf(BusinessException.class);
        verify(memberAdminQueryRepository, never()).countDaily(any(), any());
    }

    private MemberAdminRow row(UUID id, LocalDateTime createdAt) {
        return new MemberAdminRow(id, OAuthProvider.GOOGLE, "u@moodi.kr", "nick", "KR", MemberAdminStatus.ACTIVE,
                createdAt, null, null);
    }
}
