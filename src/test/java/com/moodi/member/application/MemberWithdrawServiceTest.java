package com.moodi.member.application;

import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberAgreementRepository;
import com.moodi.member.domain.MemberPreferredMoodRepository;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.MemberStatus;
import com.moodi.member.domain.RefreshTokenRepository;
import com.moodi.member.support.MemberFixture;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawServiceTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-31T10:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberPreferredMoodRepository memberPreferredMoodRepository;

    @Mock
    private MemberAgreementRepository memberAgreementRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private MemberWithdrawService memberWithdrawService;

    private MemberWithdrawService service() {
        if (memberWithdrawService == null) {
            memberWithdrawService = new MemberWithdrawService(
                    memberRepository, memberPreferredMoodRepository,
                    memberAgreementRepository, refreshTokenRepository, FIXED_CLOCK);
        }
        return memberWithdrawService;
    }

    @Test
    @DisplayName("탈퇴하면 개인정보가 비워지고 deletedAt이 기록된다")
    void withdraw_clears_personal_data() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        service().withdraw(MEMBER_ID);

        assertThat(member.isWithdrawn()).isTrue();
        assertThat(member.getDeletedAt()).isNotNull();
        assertThat(member.getEmail()).isNull();
        assertThat(member.getNickname()).isNull();
        assertThat(member.getCountry()).isNull();
        assertThat(member.getBirthYear()).isNull();
        assertThat(member.getGender()).isNull();
    }

    @Test
    @DisplayName("탈퇴해도 소셜 계정 식별자는 남는다 - 재로그인 시 복구에 쓰인다")
    void withdraw_keeps_provider_identity() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        service().withdraw(MEMBER_ID);

        assertThat(member.getProvider()).isNotNull();
        assertThat(member.getProviderId()).isNotNull();
    }

    @Test
    @DisplayName("탈퇴하면 상태가 PENDING으로 되돌아간다")
    void withdraw_resets_status_to_pending() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        service().withdraw(MEMBER_ID);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
    }

    @Test
    @DisplayName("탈퇴하면 선호 무드·약관 동의·리프레시 토큰이 삭제된다")
    void withdraw_deletes_member_owned_data() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.active()));

        service().withdraw(MEMBER_ID);

        verify(memberPreferredMoodRepository).deleteByMemberId(MEMBER_ID);
        verify(memberAgreementRepository).deleteByMemberId(MEMBER_ID);
        verify(refreshTokenRepository).deleteByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("이미 탈퇴한 회원은 다시 탈퇴할 수 없다")
    void withdraw_twice_throws() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        service().withdraw(MEMBER_ID);

        assertThatThrownBy(() -> service().withdraw(MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원은 탈퇴에 실패한다")
    void withdraw_with_unknown_member_throws() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().withdraw(MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
