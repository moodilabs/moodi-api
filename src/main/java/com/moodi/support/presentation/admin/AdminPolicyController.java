package com.moodi.support.presentation.admin;

import com.moodi.shared.auth.AdminRequired;
import com.moodi.shared.response.SuccessResponse;
import com.moodi.support.application.PolicyAdminService;
import com.moodi.support.domain.PolicyType;
import com.moodi.support.presentation.dto.admin.AdminPolicyDetailResponse;
import com.moodi.support.presentation.dto.admin.AdminPolicyRequest;
import com.moodi.support.presentation.dto.admin.AdminPolicySummaryResponse;
import com.moodi.support.presentation.dto.admin.IdResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AdminRequired
@RestController
@RequestMapping("/api/admin/policies")
public class AdminPolicyController {

    private final PolicyAdminService policyAdminService;

    public AdminPolicyController(PolicyAdminService policyAdminService) {
        this.policyAdminService = policyAdminService;
    }

    @GetMapping
    public SuccessResponse<List<AdminPolicySummaryResponse>> getPolicies(
            @RequestParam(required = false) PolicyType type) {
        return SuccessResponse.of(policyAdminService.getAll(type).stream()
                .map(AdminPolicySummaryResponse::from).toList());
    }

    @GetMapping("/{policyId}")
    public SuccessResponse<AdminPolicyDetailResponse> getPolicy(@PathVariable Long policyId) {
        return SuccessResponse.of(AdminPolicyDetailResponse.from(policyAdminService.get(policyId)));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public SuccessResponse<IdResponse> create(@Valid @RequestBody AdminPolicyRequest request) {
        return SuccessResponse.of(new IdResponse(policyAdminService.create(request.toCommand())));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{policyId}")
    public void update(@PathVariable Long policyId, @Valid @RequestBody AdminPolicyRequest request) {
        policyAdminService.update(policyId, request.toCommand());
    }

    public record PublicationRequest(@jakarta.validation.constraints.NotNull Boolean enabled,
                                     @jakarta.validation.constraints.NotNull Boolean visible) {}

    @org.springframework.web.bind.annotation.PatchMapping("/{policyId}/publication")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publication(@PathVariable Long policyId, @Valid @RequestBody PublicationRequest request) {
        policyAdminService.changePublication(policyId, request.enabled(), request.visible());
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{policyId}")
    public void delete(@PathVariable Long policyId) {
        policyAdminService.delete(policyId);
    }
}
