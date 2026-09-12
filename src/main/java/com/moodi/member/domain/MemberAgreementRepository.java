package com.moodi.member.domain;

import java.util.List;
import java.util.UUID;

public interface MemberAgreementRepository {

    MemberAgreement save(MemberAgreement agreement);

    List<MemberAgreement> findByMemberId(UUID memberId);

    void deleteByMemberId(UUID memberId);
}
