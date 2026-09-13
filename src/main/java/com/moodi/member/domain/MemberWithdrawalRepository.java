package com.moodi.member.domain;

import java.util.Optional;
import java.util.UUID;

public interface MemberWithdrawalRepository {

    MemberWithdrawal save(MemberWithdrawal withdrawal);

    Optional<MemberWithdrawal> findFirstByMemberIdOrderByCreatedAtDesc(UUID memberId);
}
