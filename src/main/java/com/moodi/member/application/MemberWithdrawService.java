package com.moodi.member.application;

import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberAgreementRepository;
import com.moodi.member.domain.MemberPreferredMoodRepository;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.RefreshTokenRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 회원 탈퇴(소프트 삭제).
 * <p>
 * `bookmark`·`route`가 `member(id)`를 FK로 참조하고 `ON DELETE CASCADE`가 없어 행을 지울 수 없다.
 * 개인정보만 비우고 `deletedAt`을 찍어 비활성화하며, 북마크·루트·피드 열람 이력은 남는다.
 * 같은 소셜 계정으로 다시 로그인하면 {@link AuthService}가 복구한다.
 */
@Service
@Transactional
public class MemberWithdrawService {

    private final MemberRepository memberRepository;
    private final MemberPreferredMoodRepository memberPreferredMoodRepository;
    private final MemberAgreementRepository memberAgreementRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public MemberWithdrawService(
            MemberRepository memberRepository,
            MemberPreferredMoodRepository memberPreferredMoodRepository,
            MemberAgreementRepository memberAgreementRepository,
            RefreshTokenRepository refreshTokenRepository,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.memberPreferredMoodRepository = memberPreferredMoodRepository;
        this.memberAgreementRepository = memberAgreementRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    public void withdraw(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.withdraw(LocalDateTime.now(clock));

        memberPreferredMoodRepository.deleteByMemberId(memberId);
        memberAgreementRepository.deleteByMemberId(memberId);
        refreshTokenRepository.deleteByMemberId(memberId);
    }
}
