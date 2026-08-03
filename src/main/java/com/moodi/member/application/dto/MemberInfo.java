package com.moodi.member.application.dto;

import com.moodi.member.domain.MemberStatus;

public record MemberInfo(MemberStatus status, String nickname, boolean hasPreferredMood) {
}
