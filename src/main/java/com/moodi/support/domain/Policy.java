package com.moodi.support.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 약관 한 버전. 시행일이 지난 버전은 회원이 동의한 시점의 문서이므로 고칠 수 없다 — 오탈자도 새 버전으로.
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

    public void update(String version, String content, LocalDate effectiveAt, LocalDate today) {
        requireNotEffective(today);
        validate(this.type, version, content, effectiveAt);
        this.version = version;
        this.content = content;
        this.effectiveAt = effectiveAt;
    }

    public boolean isEffective(LocalDate today) {
        return !effectiveAt.isAfter(today);
    }

    public void requireNotEffective(LocalDate today) {
        if (isEffective(today)) {
            throw new BusinessException(ErrorCode.POLICY_ALREADY_EFFECTIVE);
        }
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
