package com.moodi.shared.support;

import com.moodi.shared.auth.AuthAdmin;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/** 관리자 엔드포인트 문서 테스트용. `@AuthAdmin`에 고정 adminId를 주입한다. 인터셉터는 별도 단위 테스트로 검증. */
public abstract class AdminRestDocsSupport extends RestDocsSupport {

    protected final UUID adminId = UUID.randomUUID();

    @Override
    protected HandlerMethodArgumentResolver[] argumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new AuthAdminArgumentResolverStub()};
    }

    private class AuthAdminArgumentResolverStub implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthAdmin.class)
                    && parameter.getParameterType().equals(UUID.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return adminId;
        }
    }
}
