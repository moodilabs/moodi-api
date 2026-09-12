package com.moodi.shared.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 관리자 액세스 토큰이 필요한 엔드포인트. 회원 토큰으로는 통과할 수 없다.
 * `role`을 지정하면 그 권한 이상만 허용한다 (`SUPER`는 모든 권한을 포함).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminRequired {

    AdminRole role() default AdminRole.OPERATOR;
}
