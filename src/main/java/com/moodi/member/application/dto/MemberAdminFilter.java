package com.moodi.member.application.dto;

import com.moodi.member.domain.OAuthProvider;

/** 어드민 회원 목록 필터. `status`는 {@link MemberAdminStatus}로 탈퇴(가상 상태)까지 포함한다. */
public record MemberAdminFilter(String keyword, MemberAdminStatus status, OAuthProvider provider) {
}
