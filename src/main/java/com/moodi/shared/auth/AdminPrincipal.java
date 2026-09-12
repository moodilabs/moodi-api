package com.moodi.shared.auth;

import java.util.UUID;

/** 관리자 액세스 토큰에서 꺼낸 신원. */
public record AdminPrincipal(UUID adminId, AdminRole role) {
}
