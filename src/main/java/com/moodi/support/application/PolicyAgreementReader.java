package com.moodi.support.application;

import java.util.Collection;
import java.util.Set;

/** 약관 버전에 동의한 회원이 있는지 회원 컨텍스트에서 읽는 포트. 동의가 있으면 그 버전은 고칠 수 없다. */
public interface PolicyAgreementReader {

    boolean hasAgreement(Long policyId);

    /** 넘긴 ID 중 동의가 한 건이라도 있는 것만 돌려준다. */
    Set<Long> findAgreedIds(Collection<Long> policyIds);
}
