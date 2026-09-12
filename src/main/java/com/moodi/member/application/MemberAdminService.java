package com.moodi.member.application;

import com.moodi.member.application.dto.MemberAdminCursor;
import com.moodi.member.application.dto.MemberAdminDetail;
import com.moodi.member.application.dto.MemberAdminFilter;
import com.moodi.member.application.dto.MemberAdminRow;
import com.moodi.member.application.dto.MemberAdminStatus;
import com.moodi.member.application.dto.MemberDailyStat;
import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberAgreement;
import com.moodi.member.domain.MemberAgreementRepository;
import com.moodi.member.domain.MemberPreferredMood;
import com.moodi.member.domain.MemberPreferredMoodRepository;
import com.moodi.member.domain.MemberRepository;
import com.moodi.member.domain.RefreshTokenRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.shared.response.CursorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 어드민의 회원 관리(`ADM-F04`). 목록·상세·정지/해제·강제 탈퇴·일별 통계. */
@Service
@Transactional(readOnly = true)
public class MemberAdminService {

    public static final int MAX_STATS_DAYS = 92;

    private final MemberRepository memberRepository;
    private final MemberAdminQueryRepository memberAdminQueryRepository;
    private final MemberAgreementRepository memberAgreementRepository;
    private final MemberPreferredMoodRepository memberPreferredMoodRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BookmarkCountReader bookmarkCountReader;
    private final RouteCountReader routeCountReader;
    private final InquiryCountReader inquiryCountReader;
    private final MemberWithdrawService memberWithdrawService;
    private final Clock clock;

    public MemberAdminService(
            MemberRepository memberRepository,
            MemberAdminQueryRepository memberAdminQueryRepository,
            MemberAgreementRepository memberAgreementRepository,
            MemberPreferredMoodRepository memberPreferredMoodRepository,
            RefreshTokenRepository refreshTokenRepository,
            BookmarkCountReader bookmarkCountReader,
            RouteCountReader routeCountReader,
            InquiryCountReader inquiryCountReader,
            MemberWithdrawService memberWithdrawService,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.memberAdminQueryRepository = memberAdminQueryRepository;
        this.memberAgreementRepository = memberAgreementRepository;
        this.memberPreferredMoodRepository = memberPreferredMoodRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.bookmarkCountReader = bookmarkCountReader;
        this.routeCountReader = routeCountReader;
        this.inquiryCountReader = inquiryCountReader;
        this.memberWithdrawService = memberWithdrawService;
        this.clock = clock;
    }

    public CursorResponse<MemberAdminRow> getMembers(MemberAdminFilter filter, String cursor, int size) {
        MemberAdminCursor parsed = MemberAdminCursor.parse(cursor);
        List<MemberAdminRow> rows = memberAdminQueryRepository.findAll(filter,
                parsed == null ? null : parsed.createdAt(),
                parsed == null ? null : parsed.id(),
                size + 1);
        boolean hasNext = rows.size() > size;
        List<MemberAdminRow> page = hasNext ? rows.subList(0, size) : rows;
        if (page.isEmpty()) {
            return CursorResponse.empty();
        }
        return CursorResponse.of(page, hasNext ? MemberAdminCursor.of(page.getLast()) : null, hasNext);
    }

    /** 탈퇴 회원도 조회된다 — 개인정보는 비워져 있고 활동 수만 남는다. */
    public MemberAdminDetail getMember(UUID memberId) {
        Member member = findMember(memberId);
        List<MemberAdminDetail.Agreement> agreements = memberAgreementRepository.findByMemberId(memberId).stream()
                .map(agreement -> new MemberAdminDetail.Agreement(agreement.getType(), agreement.isAgreed(),
                        agreement.getAgreedAt()))
                .toList();
        return new MemberAdminDetail(
                member.getId(), member.getProvider(), member.getEmail(), member.getNickname(), member.getCountry(),
                member.getBirthYear(), member.getGender(), MemberAdminStatus.of(member), member.getCreatedAt(),
                member.getDeletedAt(), member.getSuspendedAt(), member.getSuspendReason(),
                agreements,
                memberPreferredMoodRepository.findByMemberId(memberId).stream().map(MemberPreferredMood::getMood).toList(),
                bookmarkCountReader.countByMemberId(memberId),
                routeCountReader.countActiveByMemberId(memberId),
                inquiryCountReader.countByMemberId(memberId)
        );
    }

    /** 정지 시 리프레시 토큰을 지워 재발급을 막는다. 액세스 토큰은 30분 내 자연 만료. */
    @Transactional
    public void suspend(UUID memberId, String reason) {
        Member member = findMember(memberId);
        if (member.isWithdrawn()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        member.suspend(reason, LocalDateTime.now(clock));
        memberRepository.save(member);
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    @Transactional
    public void unsuspend(UUID memberId) {
        Member member = findMember(memberId);
        member.unsuspend();
        memberRepository.save(member);
    }

    /** 강제 탈퇴는 회원 본인의 탈퇴와 같은 경로를 탄다 — 삭제 범위가 갈리지 않게. */
    @Transactional
    public void withdraw(UUID memberId) {
        memberWithdrawService.withdraw(memberId);
    }

    public List<MemberDailyStat> getDailyStats(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || from.plusDays(MAX_STATS_DAYS).isBefore(to)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return memberAdminQueryRepository.countDaily(from, to);
    }

    private Member findMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
