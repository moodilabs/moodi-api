package com.moodi.shared.auth;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Component
public class AuthAdminArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthAdmin.class)
                && parameter.getParameterType().equals(UUID.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object adminId = webRequest.getAttribute(AdminAuthInterceptor.ADMIN_ID_ATTRIBUTE,
                RequestAttributes.SCOPE_REQUEST);
        if (adminId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return adminId;
    }
}
