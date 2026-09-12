package com.moodi.admin.application.dto;

import com.moodi.shared.auth.AdminRole;

public record AdminAccountCommand(String email, String password, String name, AdminRole role) {
}
