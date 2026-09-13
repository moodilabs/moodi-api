package com.moodi.member.application;

import com.moodi.member.application.dto.WithdrawalCommand;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberAgreementRepository;
import com.moodi.member.domain.MemberPreferredMoodRepository;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.MemberWithdrawal;
import com.moodi.member.domain.MemberWithdrawalRepository;
import com.moodi.member.domain.RefreshTokenRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.event.MemberWithdrawnEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 회원 탈퇴(`MY-03`). 화면 정책 "저장한 스팟과 생성한 루트를 영구 삭제"를 따른다.
 * <p>
 * 회원 행은 개인정보만 비우고 남긴다(통계·탈퇴 사유 보존). 다른 컨텍스트의 데이터(북마크·루트·Pick)는
 * {@link MemberWithdrawnEvent}를 같은 트랜잭션에서 받은 각 컨텍스트 리스너가 지운다.
 * 리스너 하나라도 실패하면 탈퇴 전체가 롤백된다.
 */
@Service
@Transactional
public class MemberWithdrawService {

    private final MemberRepository memberRepository;
    private final MemberPreferredMoodRepository memberPreferredMoodRepository;
    private final MemberAgreementRepository memberAgreementRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberWithdrawalRepository memberWithdrawalRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public MemberWithdrawService(
            MemberRepository memberRepository,
            MemberPreferredMoodRepository memberPreferredMoodRepository,
            MemberAgreementRepository memberAgreementRepository,
            RefreshTokenRepository refreshTokenRepository,
            MemberWithdrawalRepository memberWithdrawalRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.memberPreferredMoodRepository = memberPreferredMoodRepository;
        this.memberAgreementRepository = memberAgreementRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.memberWithdrawalRepository = memberWithdrawalRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public void withdraw(UUID memberId, WithdrawalCommand command) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        MemberWithdrawal withdrawal = MemberWithdrawal.record(memberId, command.reasons(), command.detail());
        member.withdraw(LocalDateTime.now(clock));

        memberWithdrawalRepository.save(withdrawal);
        memberPreferredMoodRepository.deleteByMemberId(memberId);
        memberAgreementRepository.deleteByMemberId(memberId);
        refreshTokenRepository.deleteByMemberId(memberId);

        eventPublisher.publishEvent(new MemberWithdrawnEvent(memberId));
    }
}
