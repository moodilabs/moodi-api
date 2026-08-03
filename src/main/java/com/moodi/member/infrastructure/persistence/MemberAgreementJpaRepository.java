package com.moodi.member.infrastructure.persistence;

import com.moodi.member.domain.MemberAgreement;
import com.moodi.member.domain.MemberAgreementRepository;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface MemberAgreementJpaRepository extends MemberAgreementRepository, Repository<MemberAgreement, UUID> {
}
