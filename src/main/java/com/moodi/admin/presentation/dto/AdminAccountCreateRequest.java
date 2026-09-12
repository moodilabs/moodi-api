package com.moodi.admin.presentation.dto;

import com.moodi.admin.application.dto.AdminAccountCommand;
import com.moodi.shared.auth.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAccountCreateRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 10, message = "비밀번호는 10자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotNull(message = "권한은 필수입니다.")
        AdminRole role
) {

    public AdminAccountCommand toCommand() {
        return new AdminAccountCommand(email, password, name, role);
    }
}
