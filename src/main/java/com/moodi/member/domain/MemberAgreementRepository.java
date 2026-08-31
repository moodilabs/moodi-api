package com.moodi.member.domain;

import java.util.UUID;

public interface MemberAgreementRepository {

    MemberAgreement save(MemberAgreement agreement);

    void deleteByMemberId(UUID memberId);
}
