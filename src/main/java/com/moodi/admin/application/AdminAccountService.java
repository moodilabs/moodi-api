package com.moodi.admin.application;

import com.moodi.admin.application.dto.AdminAccountCommand;
import com.moodi.admin.application.dto.AdminAccountInfo;
import com.moodi.admin.domain.AdminAccount;
import com.moodi.admin.domain.AdminAccountRepository;
import com.moodi.admin.domain.AdminAccountStatus;
import com.moodi.admin.domain.AdminRefreshTokenRepository;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 관리자 계정 관리(SUPER 전용). 최초 계정은 {@link #bootstrap}으로 기동 시 만든다.
 */
@Service
@Transactional
public class AdminAccountService {

    public static final int MIN_PASSWORD_LENGTH = 10;

    private final AdminAccountRepository adminAccountRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountService(
            AdminAccountRepository adminAccountRepository,
            AdminRefreshTokenRepository adminRefreshTokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.adminAccountRepository = adminAccountRepository;
        this.adminRefreshTokenRepository = adminRefreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AdminAccountInfo getMe(UUID adminId) {
        return AdminAccountInfo.from(findAccount(adminId));
    }

    @Transactional(readOnly = true)
    public List<AdminAccountInfo> getAll() {
        return adminAccountRepository.findAllByOrderByCreatedAtAsc().stream().map(AdminAccountInfo::from).toList();
    }

    public UUID create(AdminAccountCommand command) {
        validatePassword(command.password());
        String loginId = normalize(command.loginId());
        if (adminAccountRepository.existsByLoginId(loginId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ADMIN_LOGIN_ID);
        }
        AdminAccount account = AdminAccount.create(loginId, passwordEncoder.encode(command.password()),
                command.name(), command.role());
        return saveWithLoginIdConflictCheck(account).getId();
    }

    /**
     * 비활성화하면 리프레시 토큰도 지워 재발급을 막는다. 액세스 토큰은 30분 내 자연 만료.
     * 자기 자신은 비활성화할 수 없다 — 마지막 SUPER가 스스로를 잠그는 사고를 막기 위해.
     */
    public void changeStatus(UUID actorId, UUID targetId, AdminAccountStatus status) {
        if (actorId.equals(targetId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        AdminAccount account = findAccount(targetId);
        account.changeStatus(status);
        adminAccountRepository.save(account);
        if (status == AdminAccountStatus.DISABLED) {
            adminRefreshTokenRepository.deleteByAdminId(targetId);
        }
    }

    public void changeRole(UUID actorId, UUID targetId, AdminRole role) {
        if (actorId.equals(targetId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        AdminAccount account = findAccount(targetId);
        account.changeRole(role);
        adminAccountRepository.save(account);
        // 권한은 액세스 토큰 클레임에 실려 있으므로 리프레시를 끊어 다음 재발급부터 새 권한을 받게 한다.
        adminRefreshTokenRepository.deleteByAdminId(targetId);
    }

    /** 새 비밀번호는 현재 것과 달라야 한다 — 초기 비밀번호를 그대로 다시 넣어 강제 변경을 우회하지 못하게. */
    public void changePassword(UUID adminId, String currentPassword, String newPassword) {
        validatePassword(newPassword);
        AdminAccount account = findAccount(adminId);
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }
        if (currentPassword.equals(newPassword)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        account.changePassword(passwordEncoder.encode(newPassword));
        adminAccountRepository.save(account);
        adminRefreshTokenRepository.deleteByAdminId(adminId);
    }

    /**
     * 기동 시 최초 SUPER 생성. 계정이 하나라도 있으면 아무것도 하지 않는다.
     */
    public boolean bootstrap(String loginId, String password) {
        if (adminAccountRepository.count() > 0) {
            return false;
        }
        validatePassword(password);
        AdminAccount account = AdminAccount.create(normalize(loginId), passwordEncoder.encode(password),
                "bootstrap", AdminRole.SUPER);
        adminAccountRepository.save(account);
        return true;
    }

    private AdminAccount findAccount(UUID adminId) {
        return adminAccountRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_ACCOUNT_NOT_FOUND));
    }

    private String normalize(String loginId) {
        if (loginId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return loginId.trim().toLowerCase();
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private AdminAccount saveWithLoginIdConflictCheck(AdminAccount account) {
        try {
            AdminAccount saved = adminAccountRepository.save(account);
            adminAccountRepository.flush();
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_ADMIN_LOGIN_ID);
        }
    }
}
