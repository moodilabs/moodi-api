package com.moodi.support.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PolicyRepository {

    Policy save(Policy policy);

    Optional<Policy> findById(Long id);

    /** 시행일이 지난 버전 중 최신. 같은 시행일이면 나중에 등록한 것. */
    Optional<Policy> findFirstByTypeAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(PolicyType type,
                                                                                             LocalDate today);

    List<Policy> findAllByOrderByTypeAscEffectiveAtDescIdDesc();

    List<Policy> findByTypeOrderByEffectiveAtDescIdDesc(PolicyType type);

    boolean existsByTypeAndVersion(PolicyType type, String version);

    boolean existsByTypeAndVersionAndLocale(PolicyType type, String version, String locale);

    Optional<Policy> findFirstByTypeAndLocaleAndEnabledTrueAndVisibleTrueAndEffectiveAtLessThanEqualOrderByEffectiveAtDescIdDesc(PolicyType type, String locale, LocalDate today);

    void delete(Policy policy);

    void flush();
}
