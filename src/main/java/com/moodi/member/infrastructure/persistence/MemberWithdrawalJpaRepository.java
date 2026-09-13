package com.moodi.member.infrastructure.persistence;

import com.moodi.member.domain.MemberWithdrawal;
import com.moodi.member.domain.MemberWithdrawalRepository;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface MemberWithdrawalJpaRepository extends MemberWithdrawalRepository, Repository<MemberWithdrawal, UUID> {
}
