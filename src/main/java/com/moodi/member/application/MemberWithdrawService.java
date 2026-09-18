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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 회원 탈퇴(`MY-03`). 화면 정책 "저장한 스팟과 생성한 루트를 영구 삭제"를 따른다.
 * <p>
 * 회원 행은 개인정보만 비우고 남긴다(통계·탈퇴 사유 보존). 다른 컨텍스트의 데이터(북마크·루트·Pick)는
 * {@link MemberWithdrawnEvent}를 같은 트랜잭션에서 받은 각 컨텍스트 리스너가 지운다.
 * 리스너 하나라도 실패하면 탈퇴 전체가 롤백된다.
 * <p>
 * 소셜 제공자 쪽 계정 연결(Apple "Apple로 로그인" 목록)도 함께 철회한다. 철회는 외부 호출이라 실패할 수 있지만
 * 탈퇴를 막지는 않는다 — 사용자의 데이터 삭제 요구가 우선이고, 철회 실패는 로그로 남겨 운영이 추적한다.
 */
@Slf4j
@Service
@Transactional
public class MemberWithdrawService {

    private final MemberRepository memberRepository;
    private final MemberPreferredMoodRepository memberPreferredMoodRepository;
    private final MemberAgreementRepository memberAgreementRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberWithdrawalRepository memberWithdrawalRepository;
    private final SocialTokenClient socialTokenClient;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public MemberWithdrawService(
            MemberRepository memberRepository,
            MemberPreferredMoodRepository memberPreferredMoodRepository,
            MemberAgreementRepository memberAgreementRepository,
            RefreshTokenRepository refreshTokenRepository,
            MemberWithdrawalRepository memberWithdrawalRepository,
            SocialTokenClient socialTokenClient,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.memberPreferredMoodRepository = memberPreferredMoodRepository;
        this.memberAgreementRepository = memberAgreementRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.memberWithdrawalRepository = memberWithdrawalRepository;
        this.socialTokenClient = socialTokenClient;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public void withdraw(UUID memberId, WithdrawalCommand command) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.isWithdrawn()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        revokeProviderConnection(member, command.authorizationCode());

        MemberWithdrawal withdrawal = MemberWithdrawal.record(memberId, command.reasons(), command.detail());
        member.withdraw(LocalDateTime.now(clock));

        memberWithdrawalRepository.save(withdrawal);
        memberPreferredMoodRepository.deleteByMemberId(memberId);
        memberAgreementRepository.deleteByMemberId(memberId);
        refreshTokenRepository.deleteByMemberId(memberId);

        eventPublisher.publishEvent(new MemberWithdrawnEvent(memberId));
    }

    /**
     * 철회 토큰은 로그인 때 받아둔 refresh token을 우선 쓰고, 탈퇴 요청에 인가 코드가 실려 오면
     * (로그인 때 코드를 안 보냈던 회원의 재인증) 그 자리에서 교환한다. 둘 다 없으면 철회할 수 없다.
     */
    private void revokeProviderConnection(Member member, String authorizationCode) {
        Optional<String> refreshToken = resolveRefreshToken(member, authorizationCode);
        if (refreshToken.isEmpty()) {
            log.info("철회할 제공자 토큰 없음 — 로그인 때 인가 코드가 없었던 회원: provider={}, memberId={}",
                    member.getProvider(), member.getId());
            return;
        }
        boolean revoked = socialTokenClient.revoke(member.getProvider(), member.getProviderClientId(), refreshToken.get());
        if (!revoked) {
            log.error("제공자 계정 연결 철회 실패 — 사용자 제공자 설정에 앱이 남을 수 있음: provider={}, memberId={}",
                    member.getProvider(), member.getId());
        }
    }

    private Optional<String> resolveRefreshToken(Member member, String authorizationCode) {
        if (authorizationCode != null && !authorizationCode.isBlank()) {
            return socialTokenClient.exchangeRefreshToken(member.getProvider(), member.getProviderClientId(), authorizationCode);
        }
        return Optional.ofNullable(member.getProviderRefreshToken());
    }
}
