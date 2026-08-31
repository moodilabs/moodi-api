package com.moodi.member.application;

import com.moodi.member.application.dto.MemberInfo;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberPreferredMoodRepository;
import com.moodi.member.domain.MemberRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;
    private final MemberPreferredMoodRepository memberPreferredMoodRepository;

    public MemberQueryService(
            MemberRepository memberRepository,
            MemberPreferredMoodRepository memberPreferredMoodRepository
    ) {
        this.memberRepository = memberRepository;
        this.memberPreferredMoodRepository = memberPreferredMoodRepository;
    }

    /**
     * 스플래시 진입 분기(`ONB-F01`)에 쓰인다.
     * 클라이언트는 status로 온보딩 완료 여부를, hasPreferredMood로 Feed A/B를 가른다.
     * 프로필(`AUT-02`)과 약관(`AUT-03`)이 2단계로 나뉘어 있어 PENDING 안에서도 되돌아갈 화면이 갈리므로,
     * 그 판단에 쓰라고 hasProfile을 함께 내려준다.
     */
    public MemberInfo getMe(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .filter(found -> !found.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        boolean hasPreferredMood = memberPreferredMoodRepository.existsByMemberId(memberId);
        return new MemberInfo(member.getStatus(), member.getNickname(), member.hasProfile(), hasPreferredMood);
    }
}
