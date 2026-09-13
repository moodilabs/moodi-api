package com.moodi.admin.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** 회원 닉네임·이메일은 조회 시점의 member 테이블 값 — 탈퇴 회원은 null. */
public record ApiRequestLogItem(Long id, UUID memberId, String memberNickname, String memberEmail, String method,
                                String path, int statusCode, int durationMs, String requestId,
                                LocalDateTime createdAt) {
}
