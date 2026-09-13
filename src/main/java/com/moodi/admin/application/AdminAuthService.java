package com.moodi.admin.application;

import com.moodi.admin.application.dto.AdminLoginResult;
import com.moodi.admin.application.dto.AdminTokenPair;
import com.moodi.admin.domain.AdminAccount;
import com.moodi.admin.domain.AdminAccountRepository;
import com.moodi.admin.domain.AdminRefreshToken;
import com.moodi.admin.domain.AdminRefreshTokenRepository;
import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class AdminAuthService {

    private final AdminAccountRepository adminAccountRepository;
    private final AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminTokenProvider adminTokenProvider;
    private final AdminLoginPolicy loginPolicy;
    private final Clock clock;

    public AdminAuthService(
            AdminAccountRepository adminAccountRepository,
            AdminRefreshTokenRepository adminRefreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AdminTokenProvider adminTokenProvider,
            AdminLoginPolicy loginPolicy,
            Clock clock
    ) {
        this.adminAccountRepository = adminAccountRepository;
        this.adminRefreshTokenRepository = adminRefreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminTokenProvider = adminTokenProvider;
        this.loginPolicy = loginPolicy;
        this.clock = clock;
    }

    /**
     * 아이디가 없어도, 비밀번호가 틀려도 같은 {@code ADMIN_LOGIN_FAILED}를 돌려준다 — 계정 존재 여부를 노출하지 않기 위해.
     * 실패 기록은 계정이 있을 때만 남는다.
     */
    public AdminLoginResult login(String loginId, String password) {
        LocalDateTime now = LocalDateTime.now(clock);
        AdminAccount account = adminAccountRepository.findByLoginId(loginId == null ? null : loginId.trim().toLowerCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED));

        if (account.isLocked(now)) {
            throw new BusinessException(ErrorCode.ADMIN_ACCOUNT_LOCKED);
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            account.recordLoginFailure(now, loginPolicy.maxFailures(), loginPolicy.lockMinutes());
            adminAccountRepository.save(account);
            throw new BusinessException(ErrorCode.ADMIN_LOGIN_FAILED);
        }
        if (!account.isActive()) {
            throw new BusinessException(ErrorCode.ADMIN_ACCOUNT_DISABLED);
        }

        account.recordLoginSuccess(now);
        adminAccountRepository.save(account);
        AdminTokenPair tokens = issueTokens(account);
        return new AdminLoginResult(tokens.accessToken(), tokens.refreshToken(), account.getRole(),
                account.isPasswordChangeRequired());
    }

    /**
     * 재발급 시 권한·상태를 DB에서 다시 읽는다 — 권한이 바뀌거나 비활성화된 계정이 옛 토큰으로 이어가지 못하게.
     */
    public AdminLoginResult reissue(String refreshToken) {
        UUID adminId = adminTokenProvider.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        AdminRefreshToken stored = adminRefreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (stored.isExpired(LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        AdminAccount account = adminAccountRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (!account.isActive()) {
            throw new BusinessException(ErrorCode.ADMIN_ACCOUNT_DISABLED);
        }

        adminRefreshTokenRepository.deleteByToken(refreshToken);
        AdminTokenPair tokens = issueTokens(account);
        return new AdminLoginResult(tokens.accessToken(), tokens.refreshToken(), account.getRole(),
                account.isPasswordChangeRequired());
    }

    public void logout(UUID adminId) {
        adminRefreshTokenRepository.deleteByAdminId(adminId);
    }

    private AdminTokenPair issueTokens(AdminAccount account) {
        AdminTokenPair tokens = adminTokenProvider.issue(account.getId(), account.getRole());
        adminRefreshTokenRepository.save(
                AdminRefreshToken.issue(account.getId(), tokens.refreshToken(), tokens.refreshTokenExpiresAt()));
        return tokens;
    }
}
