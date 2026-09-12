package com.moodi.member.presentation.dto.admin;

import com.moodi.member.application.dto.MemberDailyStat;

import java.time.LocalDate;

public record AdminMemberDailyStatResponse(LocalDate date, long signups, long withdrawals) {

    public static AdminMemberDailyStatResponse from(MemberDailyStat stat) {
        return new AdminMemberDailyStatResponse(stat.date(), stat.signups(), stat.withdrawals());
    }
}
