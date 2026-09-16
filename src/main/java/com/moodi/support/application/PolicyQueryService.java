package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.application.dto.PolicyDetail;
import com.moodi.support.application.dto.PolicySummary;
import com.moodi.support.domain.Policy;
import com.moodi.support.domain.PolicyRepository;
import com.moodi.support.domain.PolicyType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 앱의 약관 조회(`MY-07`). 종류별로 "현재 적용 중인" 버전 하나만 보여준다.
 */
@Service
@Transactional(readOnly = true)
public class PolicyQueryService {

    private final PolicyRepository policyRepository;
    private final Clock clock;

    public PolicyQueryService(PolicyRepository policyRepository, Clock clock) {
        this.policyRepository = policyRepository;
        this.clock = clock;
    }

    /**
     * 종류 순서는 화면(`MY-07-01`)의 나열 순서인 enum 선언 순서를 따른다. 적용본이 없는 종류는 뺀다.
     */
    public List<PolicySummary> getCurrentPolicies() {
        LocalDate today = LocalDate.now(clock);
        return Arrays.stream(PolicyType.values())
                .map(type -> findCurrent(type, today))
                .flatMap(Optional::stream)
                .map(PolicySummary::from)
                .toList();
    }

    public PolicyDetail getCurrentPolicy(PolicyType type) {
        return findCurrent(type, LocalDate.now(clock))
                .map(PolicyDetail::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
    }

    public List<PolicySummary> getCurrentPolicies(String locale) {
        Policy.validateLocale(locale);
        if ("en-US".equals(locale)) return getCurrentPolicies();
        return Arrays.stream(PolicyType.values()).map(type -> findCurrent(type, locale))
                .flatMap(Optional::stream).map(PolicySummary::from).toList();
    }

    public PolicyDetail getCurrentPolicy(PolicyType type, String locale) {
        Policy.validateLocale(locale);
        if ("en-US".equals(locale)) return getCurrentPolicy(type);
        return findCurrent(type, locale).map(PolicyDetail::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
    }

    private Optional<Policy> findCurrent(PolicyType type, String locale) {
        return policyRepository.findFirstByTypeAndLocaleAndEnabledTrueAndVisibleTrueAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(type, locale, LocalDate.now(clock));
    }

    private Optional<Policy> findCurrent(PolicyType type, LocalDate today) {
        return policyRepository.findFirstByTypeAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(type, today);
    }
}
