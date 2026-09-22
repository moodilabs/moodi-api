package com.moodi.support.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 약관 한 버전. 시행 여부·동의 여부와 무관하게 언제든 수정·삭제할 수 있다 — 회원 동의 기록은 동의 시점의 버전·언어를 스냅샷으로 들고 있다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Policy extends BaseEntity {

    public static final int MAX_VERSION_LENGTH = 20;
    public static final int MAX_CONTENT_LENGTH = 100_000;

    private Long id;
    private PolicyType type;
    private String version;
    private String content;
    private LocalDate effectiveAt;
    private String locale = "en-US";
    private boolean enabled = true;
    private boolean visible = true;

    public void configure(String locale, boolean enabled, boolean visible) {
        validateLocale(locale);
        this.locale = locale;
        this.enabled = enabled;
        this.visible = visible;
    }

    public void changePublication(boolean enabled, boolean visible) {
        this.enabled = enabled;
        this.visible = visible;
    }

    public static void validateLocale(String locale) {
        if (!"ko-KR".equals(locale) && !"en-US".equals(locale)) throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    public static Policy create(PolicyType type, String version, String content, LocalDate effectiveAt,
                                String locale, boolean enabled, boolean visible) {
        validateLocale(locale);
        Policy policy = create(type, version, content, effectiveAt);
        policy.locale = locale; policy.enabled = enabled; policy.visible = visible;
        return policy;
    }

    private Policy(PolicyType type, String version, String content, LocalDate effectiveAt) {
        validate(type, version, content, effectiveAt);
        this.type = type;
        this.version = version;
        this.content = content;
        this.effectiveAt = effectiveAt;
    }

    public static Policy create(PolicyType type, String version, String content, LocalDate effectiveAt) {
        return new Policy(type, version, content, effectiveAt);
    }

    public void update(String version, String content, LocalDate effectiveAt) {
        validate(this.type, version, content, effectiveAt);
        this.version = version;
        this.content = content;
        this.effectiveAt = effectiveAt;
    }

    public boolean isEffective(LocalDate today) {
        return !effectiveAt.isAfter(today);
    }

    private static void validate(PolicyType type, String version, String content, LocalDate effectiveAt) {
        if (type == null || effectiveAt == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (version == null || version.isBlank() || version.length() > MAX_VERSION_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
