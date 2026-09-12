package com.moodi.member.application;

import com.moodi.member.application.dto.MemberInfo;
import com.moodi.member.application.dto.MemberSummary;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberPreferredMoodRepository;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.MemberStatus;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.member.support.MemberFixture;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceTest {

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberPreferredMoodRepository memberPreferredMoodRepository;

    @Mock
    private BookmarkCountReader bookmarkCountReader;

    @Mock
    private RouteCountReader routeCountReader;

    @InjectMocks
    private MemberQueryService memberQueryService;

    @Test
    @DisplayName("온보딩을 마치고 무드를 설정한 회원 조회")
    void get_me_returns_active_member_with_preferred_mood() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.active()));
        when(memberPreferredMoodRepository.existsByMemberId(MEMBER_ID)).thenReturn(true);

        MemberInfo info = memberQueryService.getMe(MEMBER_ID);

        assertThat(info.status()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(info.nickname()).isEqualTo("moodi_user");
        assertThat(info.hasProfile()).isTrue();
        assertThat(info.hasPreferredMood()).isTrue();
    }

    @Test
    @DisplayName("무드 미설정 회원은 hasPreferredMood가 false다")
    void get_me_returns_false_when_no_preferred_mood() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.active()));
        when(memberPreferredMoodRepository.existsByMemberId(MEMBER_ID)).thenReturn(false);

        MemberInfo info = memberQueryService.getMe(MEMBER_ID);

        assertThat(info.hasPreferredMood()).isFalse();
    }

    @Test
    @DisplayName("온보딩 전 회원은 PENDING과 null 닉네임으로 조회된다")
    void get_me_returns_pending_member() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.create()));
        when(memberPreferredMoodRepository.existsByMemberId(MEMBER_ID)).thenReturn(false);

        MemberInfo info = memberQueryService.getMe(MEMBER_ID);

        assertThat(info.status()).isEqualTo(MemberStatus.PENDING);
        assertThat(info.nickname()).isNull();
        assertThat(info.hasProfile()).isFalse();
    }

    @Test
    @DisplayName("프로필만 저장하고 약관 동의 전인 회원은 PENDING이면서 hasProfile이 true다")
    void get_me_returns_pending_member_with_profile() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.withProfile()));
        when(memberPreferredMoodRepository.existsByMemberId(MEMBER_ID)).thenReturn(false);

        MemberInfo info = memberQueryService.getMe(MEMBER_ID);

        assertThat(info.status()).isEqualTo(MemberStatus.PENDING);
        assertThat(info.hasProfile()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 조회에 실패한다")
    void get_me_with_unknown_member_throws() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberQueryService.getMe(MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("내 정보 요약은 프로필과 북마크·루트 수를 함께 돌려준다")
    void get_summary_returns_profile_with_counts() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.active()));
        when(bookmarkCountReader.countByMemberId(MEMBER_ID)).thenReturn(36L);
        when(routeCountReader.countActiveByMemberId(MEMBER_ID)).thenReturn(6L);

        MemberSummary summary = memberQueryService.getSummary(MEMBER_ID);

        assertThat(summary.nickname()).isEqualTo("moodi_user");
        assertThat(summary.country()).isEqualTo("KR");
        assertThat(summary.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(summary.email()).isEqualTo("user@moodi.kr");
        assertThat(summary.savedSpotCount()).isEqualTo(36L);
        assertThat(summary.routeCount()).isEqualTo(6L);
    }

    @Test
    @DisplayName("탈퇴한 회원의 요약은 조회할 수 없다")
    void get_summary_with_withdrawn_member_throws() {
        Member withdrawn = MemberFixture.active();
        withdrawn.withdraw(LocalDateTime.of(2026, 8, 10, 0, 0));
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(withdrawn));

        assertThatThrownBy(() -> memberQueryService.getSummary(MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
