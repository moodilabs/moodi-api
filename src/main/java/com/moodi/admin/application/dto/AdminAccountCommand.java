package com.moodi.admin.application.dto;

import com.moodi.shared.auth.AdminRole;

public record AdminAccountCommand(String loginId, String password, String name, AdminRole role) {
}
