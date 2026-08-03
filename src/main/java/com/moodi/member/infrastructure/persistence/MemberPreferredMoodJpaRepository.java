package com.moodi.member.infrastructure.persistence;

import com.moodi.member.domain.MemberPreferredMood;
import com.moodi.member.domain.MemberPreferredMoodRepository;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface MemberPreferredMoodJpaRepository
        extends MemberPreferredMoodRepository, Repository<MemberPreferredMood, UUID> {
}
