package com.moodi.member.application;

import com.moodi.member.application.dto.AgreementCommand;
import com.moodi.member.application.dto.ProfileCommand;
import com.moodi.member.domain.AgreementType;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberAgreement;
import com.moodi.member.domain.MemberAgreementRepository;
import com.moodi.member.domain.MemberRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MemberOnboardingService {

    private final MemberRepository memberRepository;
    private final MemberAgreementRepository memberAgreementRepository;
    private final Clock clock;

    public MemberOnboardingService(
            MemberRepository memberRepository,
            MemberAgreementRepository memberAgreementRepository,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.memberAgreementRepository = memberAgreementRepository;
        this.clock = clock;
    }

    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }

    @Transactional
    public void updateProfile(UUID memberId, ProfileCommand command) {
        Member member = findMember(memberId);
        validateNicknameAvailable(member, command.nickname());
        member.updateProfile(
                command.nickname(),
                command.country(),
                command.birthYear(),
                command.gender(),
                Year.now(clock).getValue()
        );
        memberRepository.save(member);
    }

    @Transactional
    public void agree(UUID memberId, AgreementCommand command) {
        validateRequiredAgreements(command);
        Member member = findMember(memberId);
        member.activate();
        toAgreements(memberId, command).forEach(memberAgreementRepository::save);
        memberRepository.save(member);
    }

    private Member findMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 프로필 단계는 [뒤로가기] 후 재제출이 가능하므로, 자기 자신의 닉네임은 중복으로 보지 않는다.
     */
    private void validateNicknameAvailable(Member member, String nickname) {
        if (nickname == null || nickname.equals(member.getNickname())) {
            return;
        }
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    private void validateRequiredAgreements(AgreementCommand command) {
        boolean allAgreed = AgreementType.requiredTypes().stream().allMatch(command::isAgreed);
        if (!allAgreed) {
            throw new BusinessException(ErrorCode.REQUIRED_AGREEMENT_MISSING);
        }
    }

    private List<MemberAgreement> toAgreements(UUID memberId, AgreementCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        return Arrays.stream(AgreementType.values())
                .map(type -> MemberAgreement.of(memberId, type, command.isAgreed(type), now))
                .toList();
    }
}
