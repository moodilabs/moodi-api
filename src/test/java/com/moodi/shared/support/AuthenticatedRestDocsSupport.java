package com.moodi.shared.support;

import com.moodi.shared.auth.AuthMember;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

public abstract class AuthenticatedRestDocsSupport extends RestDocsSupport {

    protected final UUID memberId = UUID.randomUUID();

    @Override
    protected HandlerMethodArgumentResolver[] argumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new AuthMemberArgumentResolverStub()};
    }

    private class AuthMemberArgumentResolverStub implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthMember.class)
                    && parameter.getParameterType().equals(UUID.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return memberId;
        }
    }
}
