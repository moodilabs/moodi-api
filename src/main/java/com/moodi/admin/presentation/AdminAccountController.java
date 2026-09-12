package com.moodi.admin.presentation;

import com.moodi.admin.application.AdminAccountService;
import com.moodi.admin.presentation.dto.AdminAccountCreateRequest;
import com.moodi.admin.presentation.dto.AdminAccountResponse;
import com.moodi.admin.presentation.dto.AdminAccountRoleRequest;
import com.moodi.admin.presentation.dto.AdminAccountStatusRequest;
import com.moodi.admin.presentation.dto.AdminIdResponse;
import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.auth.AdminRole;
import com.moodi.shared.auth.AuthAdmin;
import com.moodi.shared.response.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 관리자 계정 관리. SUPER만. */
@AdminRequired(role = AdminRole.SUPER)
@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping
    public SuccessResponse<List<AdminAccountResponse>> getAccounts() {
        return SuccessResponse.of(adminAccountService.getAll().stream().map(AdminAccountResponse::from).toList());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public SuccessResponse<AdminIdResponse> create(@Valid @RequestBody AdminAccountCreateRequest request) {
        return SuccessResponse.of(new AdminIdResponse(adminAccountService.create(request.toCommand())));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{accountId}/status")
    public void changeStatus(@AuthAdmin UUID adminId, @PathVariable UUID accountId,
                             @Valid @RequestBody AdminAccountStatusRequest request) {
        adminAccountService.changeStatus(adminId, accountId, request.status());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{accountId}/role")
    public void changeRole(@AuthAdmin UUID adminId, @PathVariable UUID accountId,
                           @Valid @RequestBody AdminAccountRoleRequest request) {
        adminAccountService.changeRole(adminId, accountId, request.role());
    }
}
