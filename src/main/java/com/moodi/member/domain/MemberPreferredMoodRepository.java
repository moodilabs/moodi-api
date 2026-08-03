package com.moodi.member.domain;

import java.util.UUID;

public interface MemberPreferredMoodRepository {

    MemberPreferredMood save(MemberPreferredMood preferredMood);

    boolean existsByMemberId(UUID memberId);

    void deleteByMemberId(UUID memberId);
}
