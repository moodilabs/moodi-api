package com.moodi.admin.infrastructure;

import com.moodi.admin.application.AdminAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 최초 관리자 생성. Flyway에 비밀번호를 심지 않으려고 환경변수(`ADMIN_BOOTSTRAP_EMAIL`/`PASSWORD`)로 받는다.
 * 계정이 하나라도 있으면 아무것도 하지 않으므로 이후 배포에서 값이 남아 있어도 무해하다.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminAccountService adminAccountService;
    private final String email;
    private final String password;

    public AdminBootstrapRunner(
            AdminAccountService adminAccountService,
            @Value("${admin.bootstrap.email:}") String email,
            @Value("${admin.bootstrap.password:}") String password
    ) {
        this.adminAccountService = adminAccountService;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (adminAccountService.bootstrap(email, password)) {
            log.info("최초 관리자 계정을 생성했습니다: {}", email);
        }
    }
}
