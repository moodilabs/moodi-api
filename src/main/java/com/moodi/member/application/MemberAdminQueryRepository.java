package com.moodi.member.application;

import com.moodi.member.application.dto.MemberAdminFilter;
import com.moodi.member.application.dto.MemberAdminRow;
import com.moodi.member.application.dto.MemberDailyStat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 어드민 회원 목록·통계 조회 포트. 목록은 `createdAt DESC, id DESC`, `limit`만큼 돌려준다. */
public interface MemberAdminQueryRepository {

    List<MemberAdminRow> findAll(MemberAdminFilter filter, LocalDateTime cursorCreatedAt, UUID cursorId, int limit);

    /** `from`~`to`(포함) 일별 가입·탈퇴 수. 활동이 없는 날은 빠진다. */
    List<MemberDailyStat> countDaily(LocalDate from, LocalDate to);
}
