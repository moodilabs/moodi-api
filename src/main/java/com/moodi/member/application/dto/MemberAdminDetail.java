package com.moodi.member.application.dto;

import com.moodi.member.domain.AgreementType;
import com.moodi.member.domain.Gender;
import com.moodi.member.domain.OAuthProvider;
import com.moodi.member.domain.WithdrawalReason;
import com.moodi.shared.mood.MoodTag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MemberAdminDetail(
        UUID id,
        OAuthProvider provider,
        String email,
        String nickname,
        String country,
        Integer birthYear,
        Gender gender,
        MemberAdminStatus status,
        LocalDateTime createdAt,
        LocalDateTime deletedAt,
        LocalDateTime suspendedAt,
        String suspendReason,
        Withdrawal withdrawal,
        List<Agreement> agreements,
        List<MoodTag> preferredMoods,
        long savedSpotCount,
        long routeCount,
        long inquiryCount
) {

    public record Agreement(AgreementType type, boolean agreed, LocalDateTime agreedAt,
                                    Long policyId, String policyVersion, String policyLocale) {
        public Agreement(AgreementType type, boolean agreed, LocalDateTime agreedAt) { this(type, agreed, agreedAt, null, null, null); }
    }

    /** 탈퇴 회원의 최근 탈퇴 사유. 탈퇴 전이면 null. */
    public record Withdrawal(Set<WithdrawalReason> reasons, String detail, LocalDateTime withdrawnAt) {}
}
