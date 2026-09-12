package com.moodi.support.application;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** 어드민 문의 목록에 회원 닉네임·이메일을 붙이기 위한 포트. 탈퇴 회원은 비어 있는 값으로 온다. */
public interface MemberSummaryReader {

    Map<UUID, MemberSummary> readByIds(Collection<UUID> memberIds);

    record MemberSummary(UUID id, String nickname, String email, boolean withdrawn) {}
}
