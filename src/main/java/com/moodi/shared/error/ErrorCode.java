package com.moodi.shared.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    OAUTH_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "OAUTH_VERIFICATION_FAILED", "소셜 로그인 검증에 실패했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),

    SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "SPOT_NOT_FOUND", "스팟을 찾을 수 없습니다."),
    SPOT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "SPOT_NOT_AVAILABLE", "북마크할 수 없는 스팟입니다."),

    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND", "루트를 찾을 수 없습니다."),
    ROUTE_INVALID_DATE_RANGE(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_INVALID_DATE_RANGE", "여행 기간은 1~5일이어야 합니다."),
    ROUTE_PAST_START_DATE(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_PAST_START_DATE", "시작일은 오늘 이후여야 합니다."),
    ROUTE_TOO_MANY_SPOTS_FOR_DAYS(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_TOO_MANY_SPOTS_FOR_DAYS", "기간 대비 스팟이 너무 많습니다. 기간을 늘리거나 스팟 수를 줄여 주세요."),
    ROUTE_DAY_SPOT_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_DAY_SPOT_LIMIT_EXCEEDED", "하루 최대 6개 스팟까지 가능합니다."),
    ROUTE_DUPLICATE_DAY_NUMBER(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_DUPLICATE_DAY_NUMBER", "일차 번호가 중복됩니다."),
    ROUTE_DUPLICATE_SPOT_SEQUENCE(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_DUPLICATE_SPOT_SEQUENCE", "스팟 순서가 중복됩니다."),
    ROUTE_INVALID_TITLE(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_INVALID_TITLE", "제목은 1~30자여야 합니다."),
    ROUTE_GENERATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_GENERATION_FAILED", "루트 생성에 실패했습니다. 스팟 수를 줄이거나 기간을 늘려 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
