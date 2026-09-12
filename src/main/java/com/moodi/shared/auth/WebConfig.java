package com.moodi.shared.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final AuthMemberArgumentResolver authMemberArgumentResolver;
    private final OptionalAuthMemberArgumentResolver optionalAuthMemberArgumentResolver;
    private final AuthAdminArgumentResolver authAdminArgumentResolver;
    private final List<String> adminAllowedOrigins;

    public WebConfig(AuthInterceptor authInterceptor,
                     AdminAuthInterceptor adminAuthInterceptor,
                     AuthMemberArgumentResolver authMemberArgumentResolver,
                     OptionalAuthMemberArgumentResolver optionalAuthMemberArgumentResolver,
                     AuthAdminArgumentResolver authAdminArgumentResolver,
                     @Value("${admin.cors.allowed-origins}") List<String> adminAllowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.authMemberArgumentResolver = authMemberArgumentResolver;
        this.optionalAuthMemberArgumentResolver = optionalAuthMemberArgumentResolver;
        this.authAdminArgumentResolver = authAdminArgumentResolver;
        this.adminAllowedOrigins = adminAllowedOrigins;
    }

    /**
     * 관리자 프론트(`admin.moodi.kr`)는 API와 다른 오리진이라 `/api/admin/**`만 CORS를 연다.
     * 앱 API는 네이티브 클라이언트가 호출하므로 열지 않는다.
     * 토큰은 Authorization 헤더로만 다루므로 쿠키(credentials)는 허용하지 않는다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/admin/**")
                .allowedOrigins(adminAllowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Request-Id")
                .exposedHeaders("X-Request-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }

    /**
     * 회원 인터셉터는 `/api/admin/**`를 제외한다 — 관리자 엔드포인트에 회원 토큰이 와도 회원으로 취급하지 않는다.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/admin/**");
        registry.addInterceptor(adminAuthInterceptor).addPathPatterns("/api/admin/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authMemberArgumentResolver);
        resolvers.add(optionalAuthMemberArgumentResolver);
        resolvers.add(authAdminArgumentResolver);
    }
}
