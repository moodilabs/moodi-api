package com.moodi.support.presentation;

import com.moodi.shared.response.SuccessResponse;
import com.moodi.support.application.PolicyQueryService;
import com.moodi.support.domain.PolicyType;
import com.moodi.support.presentation.dto.PolicyDetailResponse;
import com.moodi.support.presentation.dto.PolicyListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 약관(`MY-07`). 비회원 마이 메인에서도 진입하므로 로그인 없이 조회한다.
 */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyQueryService policyQueryService;

    public PolicyController(PolicyQueryService policyQueryService) {
        this.policyQueryService = policyQueryService;
    }

    @GetMapping
    public SuccessResponse<PolicyListResponse> getPolicies() {
        return SuccessResponse.of(PolicyListResponse.from(policyQueryService.getCurrentPolicies()));
    }

    @GetMapping("/{type}")
    public SuccessResponse<PolicyDetailResponse> getPolicy(@PathVariable PolicyType type) {
        return SuccessResponse.of(PolicyDetailResponse.from(policyQueryService.getCurrentPolicy(type)));
    }
}
