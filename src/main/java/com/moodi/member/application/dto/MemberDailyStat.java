package com.moodi.member.application.dto;

import java.time.LocalDate;

public record MemberDailyStat(LocalDate date, long signups, long withdrawals) {
}
