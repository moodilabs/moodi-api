package com.moodi.member.application;

import com.moodi.member.application.dto.WithdrawalCommand;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberWithdrawal;
import com.moodi.member.domain.MemberWithdrawalRepository;
import com.moodi.member.domain.WithdrawalReason;
import com.moodi.shared.event.MemberWithdrawnEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
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
import com.moodi.member.domain.OAuthProvider;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private MemberWithdrawalRepository memberWithdrawalRepository;

    @Mock
    private SocialTokenClient socialTokenClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private static final WithdrawalCommand COMMAND =
            new WithdrawalCommand(Set.of(WithdrawalReason.HARD_TO_USE, WithdrawalReason.OTHER), "too many taps");

    private MemberWithdrawService memberWithdrawService;

    private MemberWithdrawService service() {
        if (memberWithdrawService == null) {
            memberWithdrawService = new MemberWithdrawService(
                    memberRepository, memberPreferredMoodRepository,
                    memberAgreementRepository, refreshTokenRepository, memberWithdrawalRepository,
                    socialTokenClient, eventPublisher, FIXED_CLOCK);
        }
        return memberWithdrawService;
    }

    @Test
    @DisplayName("탈퇴하면 개인정보가 비워지고 deletedAt이 기록된다")
    void withdraw_clears_personal_data() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        service().withdraw(MEMBER_ID, COMMAND);

        assertThat(member.isWithdrawn()).isTrue();
        assertThat(member.getDeletedAt()).isNotNull();
        assertThat(member.getEmail()).isNull();
        assertThat(member.getNickname()).isNull();
        assertThat(member.getCountry()).isNull();
        assertThat(member.getBirthYear()).isNull();
        assertThat(member.getGender()).isNull();
    }

    @Test
    @DisplayName("탈퇴해도 소셜 계정 식별자는 남는다 - 재로그인 시 회원 행 재활용에 쓰인다")
    void withdraw_keeps_provider_identity() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        service().withdraw(MEMBER_ID, COMMAND);

        assertThat(member.getProvider()).isNotNull();
        assertThat(member.getProviderId()).isNotNull();
    }

    @Test
    @DisplayName("탈퇴하면 상태가 PENDING으로 되돌아간다")
    void withdraw_resets_status_to_pending() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        service().withdraw(MEMBER_ID, COMMAND);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
    }

    @Test
    @DisplayName("탈퇴하면 선호 무드·약관 동의·리프레시 토큰이 삭제된다")
    void withdraw_deletes_member_owned_data() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.active()));

        service().withdraw(MEMBER_ID, COMMAND);

        verify(memberPreferredMoodRepository).deleteByMemberId(MEMBER_ID);
        verify(memberAgreementRepository).deleteByMemberId(MEMBER_ID);
        verify(refreshTokenRepository).deleteByMemberId(MEMBER_ID);
    }

    @Test
    @DisplayName("이미 탈퇴한 회원은 다시 탈퇴할 수 없다")
    void withdraw_twice_throws() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        service().withdraw(MEMBER_ID, COMMAND);

        assertThatThrownBy(() -> service().withdraw(MEMBER_ID, COMMAND))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원은 탈퇴에 실패한다")
    void withdraw_with_unknown_member_throws() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().withdraw(MEMBER_ID, COMMAND))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("탈퇴 사유가 저장되고 탈퇴 이벤트가 발행된다")
    void withdraw_saves_reasons_and_publishes_event() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.active()));

        service().withdraw(MEMBER_ID, COMMAND);

        ArgumentCaptor<MemberWithdrawal> captor = ArgumentCaptor.forClass(MemberWithdrawal.class);
        verify(memberWithdrawalRepository).save(captor.capture());
        assertThat(captor.getValue().getReasons())
                .containsExactlyInAnyOrder(WithdrawalReason.HARD_TO_USE, WithdrawalReason.OTHER);
        assertThat(captor.getValue().getDetail()).isEqualTo("too many taps");
        verify(eventPublisher).publishEvent(new MemberWithdrawnEvent(MEMBER_ID));
    }

    @Test
    @DisplayName("탈퇴 사유가 없으면 탈퇴하지 않는다")
    void withdraw_without_reasons_throws() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service().withdraw(MEMBER_ID, new WithdrawalCommand(Set.of(), null)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.WITHDRAWAL_REASON_REQUIRED);
        assertThat(member.isWithdrawn()).isFalse();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("로그인 때 받아둔 제공자 refresh token이 있으면 탈퇴 시 계정 연결을 철회하고 토큰을 비운다")
    void withdraw_revokes_stored_provider_token() {
        Member member = MemberFixture.active();
        member.rememberProviderCredential("kr.moodi.app", "apple-refresh-token");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(socialTokenClient.revoke(OAuthProvider.GOOGLE, "kr.moodi.app", "apple-refresh-token")).thenReturn(true);

        service().withdraw(MEMBER_ID, COMMAND);

        verify(socialTokenClient).revoke(OAuthProvider.GOOGLE, "kr.moodi.app", "apple-refresh-token");
        verify(socialTokenClient, never()).exchangeRefreshToken(any(), any(), any());
        assertThat(member.getProviderRefreshToken()).isNull();
        assertThat(member.isWithdrawn()).isTrue();
    }

    @Test
    @DisplayName("탈퇴 요청에 인가 코드가 있으면 그 자리에서 교환한 토큰으로 철회한다 - 로그인 때 코드를 안 보낸 회원")
    void withdraw_exchanges_code_then_revokes() {
        Member member = MemberFixture.active();
        member.rememberProviderCredential("kr.moodi.app", null);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(socialTokenClient.exchangeRefreshToken(OAuthProvider.GOOGLE, "kr.moodi.app", "reauth-code"))
                .thenReturn(Optional.of("fresh-refresh-token"));
        when(socialTokenClient.revoke(OAuthProvider.GOOGLE, "kr.moodi.app", "fresh-refresh-token")).thenReturn(true);

        service().withdraw(MEMBER_ID, new WithdrawalCommand(COMMAND.reasons(), COMMAND.detail(), "reauth-code"));

        verify(socialTokenClient).revoke(OAuthProvider.GOOGLE, "kr.moodi.app", "fresh-refresh-token");
        assertThat(member.isWithdrawn()).isTrue();
    }

    @Test
    @DisplayName("철회할 토큰이 없으면 제공자를 호출하지 않고 탈퇴만 진행한다")
    void withdraw_without_provider_token_skips_revocation() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.active()));

        service().withdraw(MEMBER_ID, COMMAND);

        verifyNoInteractions(socialTokenClient);
        verify(memberWithdrawalRepository).save(any(MemberWithdrawal.class));
    }

    @Test
    @DisplayName("제공자 철회가 실패해도 탈퇴는 막지 않는다 - 데이터 삭제 요구가 우선")
    void withdraw_proceeds_when_revocation_fails() {
        Member member = MemberFixture.active();
        member.rememberProviderCredential("kr.moodi.app", "apple-refresh-token");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(socialTokenClient.revoke(any(), any(), any())).thenReturn(false);

        service().withdraw(MEMBER_ID, COMMAND);

        assertThat(member.isWithdrawn()).isTrue();
        verify(eventPublisher).publishEvent(any(MemberWithdrawnEvent.class));
    }

    @Test
    @DisplayName("이미 탈퇴한 회원은 제공자 철회를 다시 시도하지 않는다")
    void withdraw_twice_does_not_revoke_again() {
        Member member = MemberFixture.active();
        member.rememberProviderCredential("kr.moodi.app", "apple-refresh-token");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(socialTokenClient.revoke(any(), any(), any())).thenReturn(true);
        service().withdraw(MEMBER_ID, COMMAND);

        assertThatThrownBy(() -> service().withdraw(MEMBER_ID, COMMAND)).isInstanceOf(BusinessException.class);

        verify(socialTokenClient).revoke(any(), any(), any());
    }
}
