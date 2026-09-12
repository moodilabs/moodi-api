package com.moodi.member.application;

import com.moodi.member.application.dto.MemberInfo;
import com.moodi.member.application.dto.MemberSummary;
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
    private final BookmarkCountReader bookmarkCountReader;
    private final RouteCountReader routeCountReader;

    public MemberQueryService(
            MemberRepository memberRepository,
            MemberPreferredMoodRepository memberPreferredMoodRepository,
            BookmarkCountReader bookmarkCountReader,
            RouteCountReader routeCountReader
    ) {
        this.memberRepository = memberRepository;
        this.memberPreferredMoodRepository = memberPreferredMoodRepository;
        this.bookmarkCountReader = bookmarkCountReader;
        this.routeCountReader = routeCountReader;
    }

    /**
     * 스플래시 진입 분기(`ONB-F01`)에 쓰인다.
     * 클라이언트는 status로 온보딩 완료 여부를, hasPreferredMood로 Feed A/B를 가른다.
     * 프로필(`AUT-02`)과 약관(`AUT-03`)이 2단계로 나뉘어 있어 PENDING 안에서도 되돌아갈 화면이 갈리므로,
     * 그 판단에 쓰라고 hasProfile을 함께 내려준다.
     */
    public MemberInfo getMe(UUID memberId) {
        Member member = findActiveOrPending(memberId);
        boolean hasPreferredMood = memberPreferredMoodRepository.existsByMemberId(memberId);
        return new MemberInfo(member.getStatus(), member.getNickname(), member.hasProfile(), hasPreferredMood);
    }

    /**
     * 마이 메인(`MY-01-01`)·계정설정(`MY-02-01`)·탈퇴 1단계(`MY-03-01`)가 공유하는 요약.
     * 북마크·루트 수는 다른 컨텍스트 데이터라 포트로 읽는다.
     */
    public MemberSummary getSummary(UUID memberId) {
        Member member = findActiveOrPending(memberId);
        return new MemberSummary(
                member.getNickname(),
                member.getCountry(),
                member.getProvider(),
                member.getEmail(),
                bookmarkCountReader.countByMemberId(memberId),
                routeCountReader.countActiveByMemberId(memberId)
        );
    }

    private Member findActiveOrPending(UUID memberId) {
        return memberRepository.findById(memberId)
                .filter(found -> !found.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
