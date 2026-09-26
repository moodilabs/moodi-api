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

    ADMIN_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "ADMIN_LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."),
    ADMIN_ACCOUNT_LOCKED(HttpStatus.LOCKED, "ADMIN_ACCOUNT_LOCKED", "로그인 실패가 반복되어 잠시 잠긴 계정입니다."),
    ADMIN_ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "ADMIN_ACCOUNT_DISABLED", "비활성화된 관리자 계정입니다."),
    ADMIN_FORBIDDEN(HttpStatus.FORBIDDEN, "ADMIN_FORBIDDEN", "이 작업을 수행할 권한이 없습니다."),
    ADMIN_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_ACCOUNT_NOT_FOUND", "관리자 계정을 찾을 수 없습니다."),
    DUPLICATE_ADMIN_LOGIN_ID(HttpStatus.CONFLICT, "DUPLICATE_ADMIN_LOGIN_ID", "이미 사용 중인 관리자 아이디입니다."),
    ADMIN_PASSWORD_CHANGE_REQUIRED(HttpStatus.FORBIDDEN, "ADMIN_PASSWORD_CHANGE_REQUIRED", "초기 비밀번호를 변경한 뒤 이용할 수 있습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    WITHDRAWAL_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "WITHDRAWAL_REASON_REQUIRED", "탈퇴 사유를 1개 이상 선택해주세요."),
    MEMBER_SUSPENDED(HttpStatus.FORBIDDEN, "MEMBER_SUSPENDED", "이용이 정지된 계정입니다. 고객센터로 문의해주세요."),
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
    SPOT_NOT_FAILED(HttpStatus.CONFLICT, "SPOT_NOT_FAILED", "FAILED 상태의 스팟만 재시도할 수 있습니다."),
    SPOT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "SPOT_NOT_AVAILABLE", "북마크할 수 없는 스팟입니다."),
    SPOT_MOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "SPOT_MOOD_NOT_FOUND", "아직 무드 태깅이 되지 않은 스팟입니다."),

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
    ROUTE_INVALID_SHORT_CODE(HttpStatus.INTERNAL_SERVER_ERROR, "ROUTE_INVALID_SHORT_CODE", "공유 링크 코드 형식이 올바르지 않습니다."),
    ROUTE_SHORT_CODE_EXHAUSTED(HttpStatus.INTERNAL_SERVER_ERROR, "ROUTE_SHORT_CODE_EXHAUSTED", "공유 링크 코드를 생성하지 못했습니다. 잠시 후 다시 시도해 주세요."),
    PICK_UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "PICK_UNSUPPORTED_IMAGE_TYPE", "지원하지 않는 파일 형식이에요. JPG, PNG, HEIC만 올릴 수 있어요."),
    PICK_IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "PICK_IMAGE_TOO_LARGE", "사진 용량이 너무 커요. 10MB 이하로 올려주세요."),
    PICK_INVALID_AREA_SELECTION(HttpStatus.BAD_REQUEST, "PICK_INVALID_AREA_SELECTION", "지역은 1개 이상 5개 이하로 선택해주세요."),
    PICK_ANALYSIS_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "PICK_ANALYSIS_FAILED", "사진의 무드를 분석하지 못했어요. 다시 시도해주세요."),
    PICK_NOT_FOUND(HttpStatus.NOT_FOUND, "PICK_NOT_FOUND", "추천 결과를 찾을 수 없습니다."),
    PICK_FORBIDDEN(HttpStatus.FORBIDDEN, "PICK_FORBIDDEN", "해당 추천 결과에 대한 권한이 없습니다."),
    IMAGE_UPLOAD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "IMAGE_UPLOAD_UNAVAILABLE", "사진 업로드를 잠시 사용할 수 없어요."),

    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_NOT_FOUND", "공지사항을 찾을 수 없습니다."),
    FAQ_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "FAQ_CATEGORY_NOT_FOUND", "FAQ 유형을 찾을 수 없습니다."),
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "FAQ_NOT_FOUND", "FAQ를 찾을 수 없습니다."),
    FAQ_CATEGORY_NOT_EMPTY(HttpStatus.CONFLICT, "FAQ_CATEGORY_NOT_EMPTY", "항목이 남아 있는 FAQ 유형은 삭제할 수 없습니다."),
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "POLICY_NOT_FOUND", "약관을 찾을 수 없습니다."),
    POLICY_VERSION_DUPLICATE(HttpStatus.CONFLICT, "POLICY_VERSION_DUPLICATE", "이미 등록된 약관 버전입니다."),
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다."),
    INQUIRY_FORBIDDEN(HttpStatus.FORBIDDEN, "INQUIRY_FORBIDDEN", "해당 문의에 대한 권한이 없습니다."),
    INQUIRY_TOO_MANY_ATTACHMENTS(HttpStatus.BAD_REQUEST, "INQUIRY_TOO_MANY_ATTACHMENTS", "첨부는 5개까지 올릴 수 있어요."),
    INQUIRY_UNSUPPORTED_ATTACHMENT_TYPE(HttpStatus.BAD_REQUEST, "INQUIRY_UNSUPPORTED_ATTACHMENT_TYPE", "지원하지 않는 파일 형식이에요. 사진(JPG, PNG, HEIC), 동영상(MP4, MOV), PDF만 올릴 수 있어요."),
    INQUIRY_ATTACHMENT_TOO_LARGE(HttpStatus.BAD_REQUEST, "INQUIRY_ATTACHMENT_TOO_LARGE", "파일 용량이 너무 커요. 사진·PDF는 10MB, 동영상은 50MB 이하로 올려주세요."),
    INQUIRY_INVALID_ATTACHMENT_KEY(HttpStatus.BAD_REQUEST, "INQUIRY_INVALID_ATTACHMENT_KEY", "올바르지 않은 첨부 키입니다."),

    INVALID_CURSOR_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_CURSOR_FORMAT", "잘못된 커서 형식입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
