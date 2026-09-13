package com.moodi.admin.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 공유 커널 `WebConfig`가 admin 인프라를 알지 않도록 admin 전용 인터셉터는 여기서 따로 등록한다.
 * 비밀번호 변경 강제는 인증 인터셉터(order 0)가 adminId를 남긴 뒤에 돌아야 하므로 order를 뒤로 둔다.
 */
@Configuration
public class AdminAuditWebConfig implements WebMvcConfigurer {

    private final AdminAuditInterceptor adminAuditInterceptor;
    private final AdminPasswordChangeInterceptor adminPasswordChangeInterceptor;

    public AdminAuditWebConfig(AdminAuditInterceptor adminAuditInterceptor,
                               AdminPasswordChangeInterceptor adminPasswordChangeInterceptor) {
        this.adminAuditInterceptor = adminAuditInterceptor;
        this.adminPasswordChangeInterceptor = adminPasswordChangeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminPasswordChangeInterceptor).addPathPatterns("/api/admin/**").order(10);
        registry.addInterceptor(adminAuditInterceptor).addPathPatterns("/api/admin/**").order(20);
    }
}
