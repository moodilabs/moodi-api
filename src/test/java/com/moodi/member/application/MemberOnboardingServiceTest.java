package com.moodi.member.application;

import com.moodi.member.application.dto.AgreementCommand;
import com.moodi.member.application.dto.ProfileCommand;
import com.moodi.member.domain.AgreementType;
import com.moodi.member.domain.Gender;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberAgreement;
import com.moodi.member.domain.MemberAgreementRepository;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.MemberStatus;
import com.moodi.member.support.MemberFixture;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberOnboardingServiceTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final String NICKNAME = "moodi_user";
    private static final ProfileCommand PROFILE = new ProfileCommand(NICKNAME, "KR", 1996, Gender.FEMALE);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-03T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberAgreementRepository memberAgreementRepository;

    @Captor
    private ArgumentCaptor<MemberAgreement> agreementsCaptor;

    private MemberOnboardingService memberOnboardingService;

    @BeforeEach
    void setUp() {
        memberOnboardingService = new MemberOnboardingService(memberRepository, memberAgreementRepository, FIXED_CLOCK);
    }

    @Test
    @DisplayName("사용 중이지 않은 닉네임은 사용 가능하다")
    void nickname_availability_returns_true_when_unused() {
        when(memberRepository.existsByNickname(NICKNAME)).thenReturn(false);

        assertThat(memberOnboardingService.isNicknameAvailable(NICKNAME)).isTrue();
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임은 사용 불가능하다")
    void nickname_availability_returns_false_when_used() {
        when(memberRepository.existsByNickname(NICKNAME)).thenReturn(true);

        assertThat(memberOnboardingService.isNicknameAvailable(NICKNAME)).isFalse();
    }

    @Test
    @DisplayName("프로필 설정 성공")
    void update_profile_success() {
        Member member = MemberFixture.create();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname(NICKNAME)).thenReturn(false);

        memberOnboardingService.updateProfile(MEMBER_ID, PROFILE);

        assertThat(member.getNickname()).isEqualTo(NICKNAME);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("닉네임이 중복이면 프로필 설정에 실패한다")
    void update_profile_with_duplicate_nickname_throws() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.create()));
        when(memberRepository.existsByNickname(NICKNAME)).thenReturn(true);

        assertThatThrownBy(() -> memberOnboardingService.updateProfile(MEMBER_ID, PROFILE))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("직전에 저장한 자신의 닉네임으로 재제출하면 중복으로 보지 않는다")
    void update_profile_with_own_nickname_skips_duplicate_check() {
        Member member = MemberFixture.withProfile();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        memberOnboardingService.updateProfile(MEMBER_ID, PROFILE);

        verify(memberRepository, never()).existsByNickname(anyString());
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 프로필 설정에 실패한다")
    void update_profile_with_unknown_member_throws() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberOnboardingService.updateProfile(MEMBER_ID, PROFILE))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("필수 약관에 모두 동의하면 회원이 활성화된다")
    void agree_activates_member() {
        Member member = MemberFixture.withProfile();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        memberOnboardingService.agree(MEMBER_ID, agreementCommand(true, true, true, false));

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("약관 동의 시 미동의 항목도 함께 저장된다")
    void agree_saves_all_agreement_types() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.withProfile()));

        memberOnboardingService.agree(MEMBER_ID, agreementCommand(true, true, true, false));

        verify(memberAgreementRepository, times(AgreementType.values().length)).save(agreementsCaptor.capture());
        List<MemberAgreement> saved = agreementsCaptor.getAllValues();
        assertThat(saved).filteredOn(agreement -> agreement.getType() == AgreementType.MARKETING)
                .singleElement()
                .satisfies(agreement -> {
                    assertThat(agreement.isAgreed()).isFalse();
                    assertThat(agreement.getAgreedAt()).isNull();
                });
        assertThat(saved).filteredOn(MemberAgreement::isAgreed)
                .allSatisfy(agreement -> assertThat(agreement.getAgreedAt()).isNotNull());
    }

    @Test
    @DisplayName("필수 약관에 미동의하면 가입 완료에 실패한다")
    void agree_without_required_agreement_throws() {
        assertThatThrownBy(() -> memberOnboardingService.agree(MEMBER_ID, agreementCommand(true, true, false, true)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.REQUIRED_AGREEMENT_MISSING);

        verify(memberAgreementRepository, never()).save(any(MemberAgreement.class));
    }

    @Test
    @DisplayName("프로필 설정 전에는 가입 완료할 수 없다")
    void agree_before_profile_throws() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.create()));

        assertThatThrownBy(() -> memberOnboardingService.agree(MEMBER_ID, agreementCommand(true, true, true, true)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PROFILE_REQUIRED);
    }

    @Test
    @DisplayName("이미 가입 완료된 회원은 다시 가입 완료할 수 없다")
    void agree_on_active_member_throws() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.active()));

        assertThatThrownBy(() -> memberOnboardingService.agree(MEMBER_ID, agreementCommand(true, true, true, true)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_ONBOARDED);
    }

    private AgreementCommand agreementCommand(
            boolean termsOfService,
            boolean privacyPolicy,
            boolean ageOver14,
            boolean marketing
    ) {
        return AgreementCommand.of(Map.of(
                AgreementType.TERMS_OF_SERVICE, termsOfService,
                AgreementType.PRIVACY_POLICY, privacyPolicy,
                AgreementType.AGE_OVER_14, ageOver14,
                AgreementType.MARKETING, marketing
        ));
    }
}
