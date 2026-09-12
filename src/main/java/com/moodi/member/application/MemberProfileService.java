package com.moodi.member.application;

import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 계정설정(`MY-02`)의 프로필 변경. 온보딩({@link MemberOnboardingService})과 유스케이스가 달라 분리한다.
 */
@Service
@Transactional
public class MemberProfileService {

    private final MemberRepository memberRepository;

    public MemberProfileService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public void changeNickname(UUID memberId, String nickname) {
        Member member = findMember(memberId);
        if (memberRepository.existsByNicknameAndIdNot(nickname, memberId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        member.changeNickname(nickname);
        saveWithNicknameConflictCheck(member);
    }

    public void changeCountry(UUID memberId, String country) {
        Member member = findMember(memberId);
        member.changeCountry(country);
        memberRepository.save(member);
    }

    private Member findMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .filter(found -> !found.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 조회와 저장 사이의 닉네임 선점을 uk_member_nickname 위반으로 잡아 409로 돌려준다.
     */
    private void saveWithNicknameConflictCheck(Member member) {
        try {
            memberRepository.save(member);
            memberRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }
}
