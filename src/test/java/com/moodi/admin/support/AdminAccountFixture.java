package com.moodi.admin.support;

import com.moodi.admin.domain.AdminAccount;
import com.moodi.shared.auth.AdminRole;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

public class AdminAccountFixture {

    public static final String DEFAULT_EMAIL = "ops@moodi.kr";
    public static final String DEFAULT_PASSWORD_HASH = "$2a$10$hash";

    public static AdminAccount create() {
        return create(DEFAULT_EMAIL, AdminRole.OPERATOR);
    }

    public static AdminAccount create(String email, AdminRole role) {
        return AdminAccount.create(email, DEFAULT_PASSWORD_HASH, "운영자", role);
    }

    public static AdminAccount createWithId(UUID id, AdminRole role) {
        AdminAccount account = create(DEFAULT_EMAIL, role);
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private AdminAccountFixture() {
    }
}
