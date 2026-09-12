package com.moodi.member.application;

import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.support.MemberFixture;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberProfileService memberProfileService;

    @Test
    @DisplayName("닉네임 변경 성공")
    void change_nickname_success() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNicknameAndIdNot("new_name", MEMBER_ID)).thenReturn(false);

        memberProfileService.changeNickname(MEMBER_ID, "new_name");

        assertThat(member.getNickname()).isEqualTo("new_name");
        verify(memberRepository).save(member);
        verify(memberRepository).flush();
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로는 변경할 수 없다")
    void change_nickname_with_duplicate_throws() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNicknameAndIdNot("taken", MEMBER_ID)).thenReturn(true);

        assertThatThrownBy(() -> memberProfileService.changeNickname(MEMBER_ID, "taken"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
        assertThat(member.getNickname()).isEqualTo("moodi_user");
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("저장 중 닉네임이 선점되면 중복으로 처리한다")
    void change_nickname_with_race_condition_throws_duplicate() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNicknameAndIdNot("new_name", MEMBER_ID)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("uk_member_nickname")).when(memberRepository).flush();

        assertThatThrownBy(() -> memberProfileService.changeNickname(MEMBER_ID, "new_name"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }

    @Test
    @DisplayName("국가 변경 성공")
    void change_country_success() {
        Member member = MemberFixture.active();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        memberProfileService.changeCountry(MEMBER_ID, "CN");

        assertThat(member.getCountry()).isEqualTo("CN");
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("탈퇴한 회원은 프로필을 변경할 수 없다")
    void change_country_with_withdrawn_member_throws() {
        Member withdrawn = MemberFixture.active();
        withdrawn.withdraw(LocalDateTime.of(2026, 8, 10, 0, 0));
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(withdrawn));

        assertThatThrownBy(() -> memberProfileService.changeCountry(MEMBER_ID, "CN"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 닉네임 변경에 실패한다")
    void change_nickname_with_unknown_member_throws() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberProfileService.changeNickname(MEMBER_ID, "new_name"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
