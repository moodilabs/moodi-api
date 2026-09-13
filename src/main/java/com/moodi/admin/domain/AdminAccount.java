package com.moodi.admin.domain;

import com.moodi.shared.BaseEntity;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 관리자 계정. 회원({@code member})과는 완전히 별개의 신원이다.
 * 로그인 실패가 누적되면 잠시 잠근다 — 잠금 판단은 {@link #isLocked(LocalDateTime)}로 하고 해제는 시간 경과로 자동.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAccount extends BaseEntity {

    public static final int MAX_NAME_LENGTH = 50;

    /** 영문 소문자·숫자·밑줄 4~20자. 대문자는 소문자로 정규화해 저장한다. */
    public static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-z0-9_]{4,20}$");

    private UUID id;
    private String loginId;
    private String passwordHash;
    private String name;
    private AdminRole role;
    private AdminAccountStatus status;
    private int failedLoginCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    /** 초기 비밀번호로 로그인한 상태. 본인이 비밀번호를 바꾸면 풀린다 — 그 전엔 비밀번호 변경 외 API를 막는다. */
    private boolean passwordChangeRequired;

    private AdminAccount(String loginId, String passwordHash, String name, AdminRole role) {
        validateLoginId(loginId);
        validateName(name);
        if (passwordHash == null || passwordHash.isBlank() || role == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.status = AdminAccountStatus.ACTIVE;
        this.failedLoginCount = 0;
        this.passwordChangeRequired = true;
    }

    /** 새 계정은 항상 초기 비밀번호 상태로 시작한다 — 발급자(SUPER)가 아는 비밀번호를 본인이 바꿔야 쓸 수 있다. */
    public static AdminAccount create(String loginId, String passwordHash, String name, AdminRole role) {
        return new AdminAccount(loginId, passwordHash, name, role);
    }

    public boolean isActive() {
        return status == AdminAccountStatus.ACTIVE;
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * 로그인 성공. 실패 카운트·잠금을 풀고 마지막 로그인 시각을 남긴다.
     */
    public void recordLoginSuccess(LocalDateTime now) {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = now;
    }

    /**
     * 로그인 실패. `maxFailures`에 도달하면 `lockMinutes` 동안 잠그고 카운트를 초기화한다.
     */
    public void recordLoginFailure(LocalDateTime now, int maxFailures, int lockMinutes) {
        this.failedLoginCount++;
        if (this.failedLoginCount >= maxFailures) {
            this.lockedUntil = now.plusMinutes(lockMinutes);
            this.failedLoginCount = 0;
        }
    }

    public void changeStatus(AdminAccountStatus status) {
        if (status == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.status = status;
    }

    public void changeRole(AdminRole role) {
        if (role == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.role = role;
    }

    public void changePassword(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.passwordHash = passwordHash;
        this.passwordChangeRequired = false;
    }

    private static void validateLoginId(String loginId) {
        if (loginId == null || !LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
