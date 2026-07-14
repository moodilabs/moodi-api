package com.moodi.member.infrastructure.persistence;

import com.moodi.member.domain.Member;
import com.moodi.member.domain.MemberRepository;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface MemberJpaRepository extends MemberRepository, Repository<Member, UUID> {
}
