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

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임이에요."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME", "닉네임은 2~20자의 영문·숫자·'_'·'.'만 사용할 수 있어요."),
    INVALID_BIRTH_YEAR(HttpStatus.BAD_REQUEST, "INVALID_BIRTH_YEAR", "올바른 출생연도를 입력해주세요."),
    UNDERAGE(HttpStatus.BAD_REQUEST, "UNDERAGE", "만 14세 이상부터 가입할 수 있어요."),
    INVALID_COUNTRY(HttpStatus.BAD_REQUEST, "INVALID_COUNTRY", "올바른 국가를 선택해주세요."),
    REQUIRED_AGREEMENT_MISSING(HttpStatus.BAD_REQUEST, "REQUIRED_AGREEMENT_MISSING", "필수 약관에 모두 동의해야 가입할 수 있어요."),
    PROFILE_REQUIRED(HttpStatus.BAD_REQUEST, "PROFILE_REQUIRED", "프로필 설정을 먼저 완료해주세요."),
    ALREADY_ONBOARDED(HttpStatus.CONFLICT, "ALREADY_ONBOARDED", "이미 가입이 완료된 회원입니다."),
    INSUFFICIENT_MOOD_SELECTION(HttpStatus.BAD_REQUEST, "INSUFFICIENT_MOOD_SELECTION", "무드는 3개 이상 선택해주세요."),

    SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "SPOT_NOT_FOUND", "스팟을 찾을 수 없습니다."),
    SPOT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "SPOT_NOT_AVAILABLE", "북마크할 수 없는 스팟입니다."),

    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND", "루트를 찾을 수 없습니다."),
    ROUTE_INVALID_DATE_RANGE(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_INVALID_DATE_RANGE", "여행 기간은 1~5일이어야 합니다."),
    ROUTE_PAST_START_DATE(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_PAST_START_DATE", "시작일은 오늘 이후여야 합니다."),
    ROUTE_TOO_MANY_SPOTS_FOR_DAYS(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_TOO_MANY_SPOTS_FOR_DAYS", "기간 대비 스팟이 너무 많습니다. 기간을 늘리거나 스팟 수를 줄여 주세요."),
    ROUTE_DAY_SPOT_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_DAY_SPOT_LIMIT_EXCEEDED", "하루 최대 6개 스팟까지 가능합니다."),
    ROUTE_DUPLICATE_DAY_NUMBER(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_DUPLICATE_DAY_NUMBER", "일차 번호가 중복됩니다."),
    ROUTE_DUPLICATE_SPOT_SEQUENCE(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_DUPLICATE_SPOT_SEQUENCE", "스팟 순서가 중복됩니다."),
    ROUTE_FORBIDDEN(HttpStatus.FORBIDDEN, "ROUTE_FORBIDDEN", "해당 루트에 대한 권한이 없습니다."),
    ROUTE_INVALID_TITLE(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_INVALID_TITLE", "제목은 1~40자여야 합니다."),
    ROUTE_GENERATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_GENERATION_FAILED", "루트 생성에 실패했습니다. 스팟 수를 줄이거나 기간을 늘려 주세요."),
    PICK_UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "PICK_UNSUPPORTED_IMAGE_TYPE", "지원하지 않는 파일 형식이에요. JPG, PNG, HEIC만 올릴 수 있어요."),
    PICK_IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "PICK_IMAGE_TOO_LARGE", "사진 용량이 너무 커요. 10MB 이하로 올려주세요."),
    PICK_INVALID_AREA_SELECTION(HttpStatus.BAD_REQUEST, "PICK_INVALID_AREA_SELECTION", "지역은 1개 이상 5개 이하로 선택해주세요."),
    PICK_ANALYSIS_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "PICK_ANALYSIS_FAILED", "사진의 무드를 분석하지 못했어요. 다시 시도해주세요."),
    PICK_NOT_FOUND(HttpStatus.NOT_FOUND, "PICK_NOT_FOUND", "추천 결과를 찾을 수 없습니다."),
    PICK_FORBIDDEN(HttpStatus.FORBIDDEN, "PICK_FORBIDDEN", "해당 추천 결과에 대한 권한이 없습니다."),
    IMAGE_UPLOAD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "IMAGE_UPLOAD_UNAVAILABLE", "사진 업로드를 잠시 사용할 수 없어요."),

    INVALID_CURSOR_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_CURSOR_FORMAT", "잘못된 커서 형식입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
