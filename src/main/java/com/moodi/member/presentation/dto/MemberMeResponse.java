package com.moodi.member.presentation.dto;

import com.moodi.member.application.dto.MemberInfo;
import com.moodi.member.domain.MemberStatus;

public record MemberMeResponse(
        MemberStatus status,
        String nickname,
        boolean hasProfile,
        boolean hasPreferredMood
) {

    public static MemberMeResponse from(MemberInfo info) {
        return new MemberMeResponse(info.status(), info.nickname(), info.hasProfile(), info.hasPreferredMood());
    }
}
