package com.moodi.shared.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AuthMemberArgumentResolver authMemberArgumentResolver;
    private final OptionalAuthMemberArgumentResolver optionalAuthMemberArgumentResolver;

    public WebConfig(AuthInterceptor authInterceptor,
                     AuthMemberArgumentResolver authMemberArgumentResolver,
                     OptionalAuthMemberArgumentResolver optionalAuthMemberArgumentResolver) {
        this.authInterceptor = authInterceptor;
        this.authMemberArgumentResolver = authMemberArgumentResolver;
        this.optionalAuthMemberArgumentResolver = optionalAuthMemberArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authMemberArgumentResolver);
        resolvers.add(optionalAuthMemberArgumentResolver);
    }
}
