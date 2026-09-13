package com.moodi.admin.presentation;

import com.moodi.admin.application.AdminAccountService;
import com.moodi.admin.application.AdminAuthService;
import com.moodi.admin.presentation.dto.AdminAccountResponse;
import com.moodi.admin.presentation.dto.AdminLoginRequest;
import com.moodi.admin.presentation.dto.AdminPasswordChangeRequest;
import com.moodi.admin.presentation.dto.AdminReissueRequest;
import com.moodi.admin.presentation.dto.AdminTokenResponse;
import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.auth.AuthAdmin;
import com.moodi.shared.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AdminAccountService adminAccountService;

    public AdminAuthController(AdminAuthService adminAuthService, AdminAccountService adminAccountService) {
        this.adminAuthService = adminAuthService;
        this.adminAccountService = adminAccountService;
    }

    @PostMapping("/auth/login")
    public SuccessResponse<AdminTokenResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return SuccessResponse.of(AdminTokenResponse.from(
                adminAuthService.login(request.loginId(), request.password())));
    }

    @PostMapping("/auth/reissue")
    public SuccessResponse<AdminTokenResponse> reissue(@Valid @RequestBody AdminReissueRequest request) {
        return SuccessResponse.of(AdminTokenResponse.from(adminAuthService.reissue(request.refreshToken())));
    }

    @AdminRequired
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/auth/logout")
    public void logout(@AuthAdmin UUID adminId) {
        adminAuthService.logout(adminId);
    }

    @AdminRequired
    @GetMapping("/me")
    public SuccessResponse<AdminAccountResponse> getMe(@AuthAdmin UUID adminId) {
        return SuccessResponse.of(AdminAccountResponse.from(adminAccountService.getMe(adminId)));
    }

    @AdminRequired
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/me/password")
    public void changePassword(@AuthAdmin UUID adminId, @Valid @RequestBody AdminPasswordChangeRequest request) {
        adminAccountService.changePassword(adminId, request.currentPassword(), request.newPassword());
    }
}
