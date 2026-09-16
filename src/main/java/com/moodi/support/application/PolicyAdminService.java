package com.moodi.support.application;

import com.moodi.shared.error.BusinessException;
import com.moodi.shared.error.ErrorCode;
import com.moodi.support.application.dto.PolicyCommand;
import com.moodi.support.application.dto.PolicyDetail;
import com.moodi.support.application.dto.PolicySummary;
import com.moodi.support.domain.Policy;
import com.moodi.support.domain.PolicyRepository;
import com.moodi.support.domain.PolicyType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * 어드민의 약관 관리. 시행 전 버전만 고치거나 지울 수 있다. 컨트롤러는 관리자 인증(`ADM-F01`)과 함께 붙는다.
 */
@Service
@Transactional
public class PolicyAdminService {

    private final PolicyRepository policyRepository;
    private final Clock clock;

    public PolicyAdminService(PolicyRepository policyRepository, Clock clock) {
        this.policyRepository = policyRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PolicySummary> getAll(PolicyType type) {
        List<Policy> policies = type == null
                ? policyRepository.findAllByOrderByTypeAscEffectiveAtDescIdDesc()
                : policyRepository.findByTypeOrderByEffectiveAtDescIdDesc(type);
        return policies.stream().map(PolicySummary::from).toList();
    }

    @Transactional(readOnly = true)
    public PolicyDetail get(Long policyId) {
        return PolicyDetail.from(findPolicy(policyId));
    }

    public Long create(PolicyCommand command) {
        validateVersionAvailable(command.type(), command.version(), command.locale());
        Policy policy = Policy.create(command.type(), command.version(), command.content(), command.effectiveAt(), command.locale(), command.enabled(), command.visible());
        return saveWithVersionConflictCheck(policy).getId();
    }

    public void update(Long policyId, PolicyCommand command) {
        Policy policy = findPolicy(policyId);
        if (policy.getType() != command.type()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        if (!policy.getVersion().equals(command.version()) || !policy.getLocale().equals(command.locale())) {
            validateVersionAvailable(policy.getType(), command.version(), command.locale());
        }
        policy.configure(command.locale(), command.enabled(), command.visible(), LocalDate.now(clock));
        policy.update(command.version(), command.content(), command.effectiveAt(), LocalDate.now(clock));
        saveWithVersionConflictCheck(policy);
    }

    public void changePublication(Long policyId, boolean enabled, boolean visible) {
        Policy policy = findPolicy(policyId);
        policy.changePublication(enabled, visible);
        policyRepository.save(policy);
    }

    public void delete(Long policyId) {
        Policy policy = findPolicy(policyId);
        policy.requireNotEffective(LocalDate.now(clock));
        policyRepository.delete(policy);
    }

    private Policy findPolicy(Long policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
    }

    private void validateVersionAvailable(PolicyType type, String version, String locale) {
        if (policyRepository.existsByTypeAndVersionAndLocale(type, version, locale)) {
            throw new BusinessException(ErrorCode.POLICY_VERSION_DUPLICATE);
        }
    }

    /**
     * 조회와 저장 사이의 선점을 uk_policy_type_version 위반으로 잡아 409로 돌려준다.
     */
    private Policy saveWithVersionConflictCheck(Policy policy) {
        try {
            Policy saved = policyRepository.save(policy);
            policyRepository.flush();
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.POLICY_VERSION_DUPLICATE);
        }
    }
}
