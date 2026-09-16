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
    public SuccessResponse<PolicyListResponse> getPolicies(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "en-US") String locale) {
        return SuccessResponse.of(PolicyListResponse.from(("en-US".equals(locale) ? policyQueryService.getCurrentPolicies() : policyQueryService.getCurrentPolicies(locale))));
    }

    @GetMapping("/{type}")
    public SuccessResponse<PolicyDetailResponse> getPolicy(@PathVariable PolicyType type, @org.springframework.web.bind.annotation.RequestParam(defaultValue = "en-US") String locale) {
        return SuccessResponse.of(PolicyDetailResponse.from(("en-US".equals(locale) ? policyQueryService.getCurrentPolicy(type) : policyQueryService.getCurrentPolicy(type, locale))));
    }
}
